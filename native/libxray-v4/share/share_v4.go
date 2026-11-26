package share

import (
	"encoding/json"
	"errors"
	"io"
	"reflect"
	"strings"

	"github.com/metacubex/age"
	"github.com/metacubex/age/armor"
	"github.com/xtls/xray-core/infra/conf"
)

var (
	ErrV4ShareInputTooLarge = errors.New("profile input exceeds supported size")
	ErrV4ShareOutputTooLarge = errors.New("profile output exceeds supported size")
)

const (
	maxV4ShareInputBytes = 2 * 1024 * 1024
	maxV4ShareOutputBytes = 2 * 1024 * 1024
	maxV4ShareOutbounds = 256
	maxV4ShareDepth = 32
	maxV4ShareObjectFields = 64
	maxV4ShareArrayElements = 4096
)

var (
	v4ShareRawMessageType = reflect.TypeOf(json.RawMessage{})
	v4ShareNumberType = reflect.TypeOf(json.Number(""))
)

// ConvertShareLinksToXrayJsonV4 is the V4-specific conversion path. It keeps
// upstream V3 behavior unchanged while enforcing smaller V4 plaintext/output
// limits before the share package performs JSON marshaling.
func ConvertShareLinksToXrayJsonV4(links, secretKey string) (json.RawMessage, error) {
	text, encrypted, err := decryptShareTextV4(links, secretKey)
	if err != nil {
		return nil, err
	}
	if len(text) > maxV4ShareInputBytes {
		return nil, ErrV4ShareInputTooLarge
	}
	config, err := parseShareCandidates(text, true)
	if err != nil {
		if encrypted {
			return nil, ErrAgePlaintextUnsupported
		}
		return nil, err
	}
	config, err = filterBuildableOutboundsV4(config)
	if err != nil {
		if errors.Is(err, ErrV4ShareOutputTooLarge) {
			return nil, err
		}
		if encrypted {
			return nil, ErrAgePlaintextUnsupported
		}
		return nil, errors.New("no valid outbound found")
	}
	raw, err := marshalShareConfigJSONV4(config)
	if err != nil {
		if errors.Is(err, ErrV4ShareOutputTooLarge) {
			return nil, err
		}
		if encrypted {
			return nil, ErrAgePlaintextUnsupported
		}
		return nil, errors.New("no valid outbound found")
	}
	return raw, nil
}

func decryptShareTextV4(links, secretKey string) (string, bool, error) {
	if len(links) > maxV4ShareInputBytes {
		return "", false, ErrV4ShareInputTooLarge
	}
	text := strings.TrimSpace(FixWindowsReturn(links))
	if !strings.HasPrefix(text, ageArmorHeader) {
		return links, false, nil
	}
	if strings.TrimSpace(secretKey) == "" {
		return "", true, ErrAgeSecretKeyMissing
	}
	identity, _, err := parseNativeAgeIdentity(secretKey)
	if err != nil {
		return "", true, err
	}
	reader, err := age.Decrypt(armor.NewReader(strings.NewReader(text)), identity)
	if err != nil {
		var noMatch *age.NoIdentityMatchError
		if errors.As(err, &noMatch) {
			return "", true, ErrAgeDecryptFailed
		}
		return "", true, ErrAgeArmorMalformed
	}
	plaintext, err := io.ReadAll(io.LimitReader(reader, maxV4ShareInputBytes+1))
	if err != nil {
		return "", true, ErrAgeArmorMalformed
	}
	if len(plaintext) > maxV4ShareInputBytes {
		return "", true, ErrV4ShareInputTooLarge
	}
	return string(plaintext), true, nil
}

func filterBuildableOutboundsV4(config *conf.Config) (*conf.Config, error) {
	if config == nil || len(config.OutboundConfigs) == 0 || len(config.OutboundConfigs) > maxV4ShareOutbounds {
		return nil, ErrV4ShareOutputTooLarge
	}
	if estimateShareJSONSize(reflect.ValueOf(config.OutboundConfigs), maxV4ShareOutputBytes+1) > maxV4ShareOutputBytes {
		return nil, ErrV4ShareOutputTooLarge
	}
	raw, err := json.Marshal(config.OutboundConfigs)
	if err != nil || len(raw) > maxV4ShareOutputBytes {
		return nil, ErrV4ShareOutputTooLarge
	}
	var validationOutbounds []conf.OutboundDetourConfig
	if err := json.Unmarshal(raw, &validationOutbounds); err != nil {
		return nil, errors.New("invalid outbound data")
	}
	restoreNilRawMessages(reflect.ValueOf(&validationOutbounds))
	validOutbounds := make([]conf.OutboundDetourConfig, 0, len(config.OutboundConfigs))
	for index := range validationOutbounds {
		if _, err := validationOutbounds[index].Build(); err == nil {
			validOutbounds = append(validOutbounds, config.OutboundConfigs[index])
		}
	}
	if len(validOutbounds) == 0 {
		return nil, errors.New("no valid outbound found")
	}
	if len(validOutbounds) > maxV4ShareOutbounds {
		return nil, ErrV4ShareOutputTooLarge
	}
	config.OutboundConfigs = validOutbounds
	return config, nil
}

func marshalShareConfigJSONV4(config *conf.Config) (json.RawMessage, error) {
	if config == nil || len(config.OutboundConfigs) == 0 {
		return nil, errors.New("no valid outbound found")
	}
	if len(config.OutboundConfigs) > maxV4ShareOutbounds {
		return nil, ErrV4ShareOutputTooLarge
	}
	outbounds := make([]map[string]any, 0, len(config.OutboundConfigs))
	for _, outbound := range config.OutboundConfigs {
		if estimateShareJSONSize(reflect.ValueOf(outbound), maxV4ShareOutputBytes+1) > maxV4ShareOutputBytes {
			return nil, ErrV4ShareOutputTooLarge
		}
		source, err := marshalShareJSONObject(outbound)
		if err != nil {
			return nil, errors.New("invalid outbound data")
		}
		projected, supported := projectShareOutbound(source)
		if !supported {
			continue
		}
		if estimateShareJSONSize(reflect.ValueOf(projected), maxV4ShareOutputBytes+1) > maxV4ShareOutputBytes {
			return nil, errors.New("profile output exceeds supported size")
		}
		if err := validateProjectedShareOutbound(projected); err != nil {
			continue
		}
		outbounds = append(outbounds, projected)
		if len(outbounds) > maxV4ShareOutbounds ||
			estimateShareJSONSize(reflect.ValueOf(outbounds), maxV4ShareOutputBytes+1) > maxV4ShareOutputBytes {
			return nil, errors.New("profile output exceeds supported size")
		}
	}
	if len(outbounds) == 0 {
		return nil, errors.New("no valid outbound found")
	}
	result := map[string]any{"outbounds": outbounds}
	if estimateShareJSONSize(reflect.ValueOf(result), maxV4ShareOutputBytes+1) > maxV4ShareOutputBytes {
		return nil, errors.New("profile output exceeds supported size")
	}
	raw, err := json.Marshal(result)
	if err != nil || len(raw) > maxV4ShareOutputBytes {
		return nil, errors.New("profile output exceeds supported size")
	}
	return raw, nil
}

// estimateShareJSONSize is a conservative preflight estimator. It bounds all
// large share-package marshaling operations, including RawMessage fields.
func estimateShareJSONSize(value reflect.Value, limit int) int {
	return estimateShareJSONValue(value, limit, 0)
}

func estimateShareJSONValue(value reflect.Value, limit, depth int) int {
	if !value.IsValid() {
		return 4
	}
	if depth > maxV4ShareDepth {
		return limit + 1
	}
	if value.Type() == v4ShareRawMessageType {
		if value.Len() > limit || !json.Valid(value.Bytes()) {
			return limit + 1
		}
		return value.Len()
	}
	if value.Type() == v4ShareNumberType {
		return value.Len() + 2
	}
	if value.Kind() == reflect.Interface || value.Kind() == reflect.Pointer {
		if value.IsNil() {
			return 4
		}
		return estimateShareJSONValue(value.Elem(), limit, depth+1)
	}
	switch value.Kind() {
	case reflect.String:
		return 2 + value.Len()*6
	case reflect.Bool:
		if value.Bool() { return 4 }
		return 5
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64,
		reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64,
		reflect.Float32, reflect.Float64:
		return 32
	case reflect.Slice:
		if value.IsNil() { return 4 }
		if value.Type().Elem().Kind() == reflect.Uint8 {
			return 2 + ((value.Len()+2)/3)*4
		}
		if value.Len() > maxV4ShareArrayElements { return limit + 1 }
		total := 2
		for i := 0; i < value.Len(); i++ {
			total += 1 + estimateShareJSONValue(value.Index(i), limit, depth+1)
			if total > limit { return total }
		}
		return total
	case reflect.Array:
		if value.Len() > maxV4ShareArrayElements { return limit + 1 }
		total := 2
		for i := 0; i < value.Len(); i++ {
			total += 1 + estimateShareJSONValue(value.Index(i), limit, depth+1)
			if total > limit { return total }
		}
		return total
	case reflect.Map:
		if value.IsNil() { return 4 }
		if value.Len() > maxV4ShareObjectFields { return limit + 1 }
		total := 2
		iter := value.MapRange()
		for iter.Next() {
			key := iter.Key()
			if key.Kind() != reflect.String { return limit + 1 }
			total += 1 + estimateShareJSONValue(key, limit, depth+1) + 1 +
				estimateShareJSONValue(iter.Value(), limit, depth+1)
			if total > limit { return total }
		}
		return total
	case reflect.Struct:
		total := 2
		typeInfo := value.Type()
		fields := 0
		for i := 0; i < value.NumField(); i++ {
			field := typeInfo.Field(i)
			if field.PkgPath != "" { continue }
			tag := strings.Split(field.Tag.Get("json"), ",")[0]
			if tag == "-" { continue }
			if strings.Contains(field.Tag.Get("json"), "omitempty") && value.Field(i).IsZero() { continue }
			if tag == "" { tag = field.Name }
			fields++
			if fields > maxV4ShareObjectFields { return limit + 1 }
			total += 1 + len(tag)*6 + 3 + estimateShareJSONValue(value.Field(i), limit, depth+1)
			if total > limit { return total }
		}
		return total
	default:
		return limit + 1
	}
}

