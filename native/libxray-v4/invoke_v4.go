package libXray

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"reflect"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"unicode/utf8"

	"github.com/xtls/libxray/share"
	"github.com/xtls/libxray/xray"
)

const (
	libXrayV4APIVersion       = 4
	maxV4AbsoluteBytes        = 4 * 1024 * 1024
	maxV4ProfileBytes         = 2 * 1024 * 1024
	maxV4ControlBytes         = 64 * 1024
	maxV4NestingDepth         = 32
	maxV4ObjectFields         = 64
	maxV4ArrayElements        = 4096
	maxV4ConvertedOutbounds   = 256
)

var (
	v4RequestIDPattern = regexp.MustCompile(`^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}package libXray

import (
	"bytes"
	"encoding/json"
	"errors"
	"io"
	"reflect"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"unicode/utf8"

	"github.com/xtls/libxray/share"
	"github.com/xtls/libxray/xray"
)

const (
	libXrayV4APIVersion       = 4
	maxV4AbsoluteBytes        = 4 * 1024 * 1024
	maxV4ProfileBytes         = 2 * 1024 * 1024
	maxV4ControlBytes         = 64 * 1024
	maxV4NestingDepth         = 32
	maxV4ObjectFields         = 64
	maxV4ArrayElements        = 4096
	maxV4ConvertedOutbounds   = 256
)

var (
	)
	errV4RequestTooLarge = errors.New("request exceeds method size limit")
	v4InvokeMu sync.Mutex
)

type v4Response struct {
	APIVersion int         `json:"apiVersion"`
	RequestID  *string     `json:"requestId"`
	Method     *string     `json:"method"`
	Success    bool        `json:"success"`
	Code       string      `json:"code"`
	Data       any         `json:"data"`
	Error      *v4Error    `json:"error"`
}

type v4Error struct {
	Category  string `json:"category"`
	Retryable bool   `json:"retryable"`
}

// InvokeV4 is an app-specific, versioned entry point. The upstream Invoke v3
// implementation is intentionally left unchanged for existing consumers.
func InvokeV4(requestJSON string) (response string) {
	requestID, method := (*string)(nil), (*string)(nil)
	defer func() {
		if recover() != nil {
			response = encodeV4Failure(requestID, method, "INTERNAL_ERROR", "INTERNAL", false)
		}
	}()

	if len(requestJSON) == 0 || len(requestJSON) > maxV4AbsoluteBytes || !utf8.ValidString(requestJSON) {
		return encodeV4Failure(nil, nil, "INVALID_REQUEST", "PROTOCOL", false)
	}
	parsed, err := parseV4JSON([]byte(requestJSON))
	if err != nil {
		return encodeV4Failure(nil, nil, "INVALID_REQUEST", "PROTOCOL", false)
	}
	root, ok := parsed.(map[string]any)
	if !ok {
		return encodeV4Failure(nil, nil, "INVALID_REQUEST", "PROTOCOL", false)
	}
	if value, ok := root["requestId"].(string); ok && v4RequestIDPattern.MatchString(value) {
		requestID = &value
	}
	if value, ok := root["method"].(string); ok && len(value) <= 64 && isV4ASCII(value) {
		method = &value
	}
	if !hasExactV4Fields(root, "apiVersion", "requestId", "method", "payload") {
		return encodeV4Failure(requestID, method, "INVALID_REQUEST", "PROTOCOL", false)
	}
	if requestID == nil || method == nil {
		return encodeV4Failure(requestID, method, "INVALID_REQUEST", "PROTOCOL", false)
	}

	version, ok := root["apiVersion"].(json.Number)
	if !ok {
		return encodeV4Failure(requestID, method, "INVALID_REQUEST", "PROTOCOL", false)
	}
	versionInt, err := strconv.Atoi(string(version))
	if err != nil || versionInt != libXrayV4APIVersion {
		return encodeV4Failure(requestID, method, "UNSUPPORTED_API_VERSION", "PROTOCOL", false)
	}

	payload, ok := root["payload"].(map[string]any)
	if !ok {
		return encodeV4Failure(requestID, method, "INVALID_REQUEST", "PROTOCOL", false)
	}
	if !isV4Method(*method) {
		return encodeV4Failure(requestID, method, "UNSUPPORTED_METHOD", "PROTOCOL", false)
	}
	if *method == string(LibXrayMethodStopXray) && len(requestJSON) > maxV4ControlBytes {
		return encodeV4Failure(requestID, method, "REQUEST_TOO_LARGE", "LIMIT", false)
	}
	if err := validateV4Payload(*method, payload); err != nil {
		if errors.Is(err, errV4RequestTooLarge) {
			return encodeV4Failure(requestID, method, "REQUEST_TOO_LARGE", "LIMIT", false)
		}
		return encodeV4Failure(requestID, method, "INVALID_REQUEST", "PROTOCOL", false)
	}

	// The mutex serializes all V4 operations, including calls made concurrently
	// from multiple JNI threads. Stop remains callable while a tunnel is active.
	v4InvokeMu.Lock()
	defer v4InvokeMu.Unlock()

	if *method == string(LibXrayMethodRunXray) && xray.GetXrayState() {
		return encodeV4Failure(requestID, method, "CORE_BUSY", "ENGINE", true)
	}
	if *method == string(LibXrayMethodTestXray) && xray.GetXrayState() {
		return encodeV4Failure(requestID, method, "CORE_BUSY", "ENGINE", true)
	}

	data, callErr := executeV4Method(*method, payload)
	if callErr != nil {
		code, category := v4MethodError(*method)
		return encodeV4Failure(requestID, method, code, category, false)
	}
	budget := maxV4ControlBytes
	if *method == string(LibXrayMethodConvertShareLinksToXrayJson) {
		budget = maxV4ProfileBytes
	}
	if err := validateV4ResultPreflight(*method, data); err != nil {
		return encodeV4Failure(requestID, method, "INTERNAL_ERROR", "INTERNAL", false)
	}
	if estimateV4JSONSize(reflect.ValueOf(data), budget+1) > budget {
		return encodeV4Failure(requestID, method, "RESPONSE_TOO_LARGE", "LIMIT", false)
	}
	dataJSON, err := json.Marshal(data)
	if err != nil || len(dataJSON) > budget {
		return encodeV4Failure(requestID, method, "RESPONSE_TOO_LARGE", "LIMIT", false)
	}
	if err := validateV4Result(*method, dataJSON); err != nil {
		return encodeV4Failure(requestID, method, "INTERNAL_ERROR", "INTERNAL", false)
	}
	envelope := v4Response{
		APIVersion: libXrayV4APIVersion,
		RequestID: requestID,
		Method: method,
		Success: true,
		Code: "OK",
		Data: json.RawMessage(dataJSON),
		Error: nil,
	}
	raw, err := json.Marshal(envelope)
	if err != nil || len(raw) > maxV4AbsoluteBytes {
		return encodeV4Failure(requestID, method, "RESPONSE_TOO_LARGE", "LIMIT", false)
	}
	return string(raw)
}

func parseV4JSON(raw []byte) (any, error) {
	decoder := json.NewDecoder(bytes.NewReader(raw))
	decoder.UseNumber()
	value, err := parseV4Value(decoder, 0)
	if err != nil {
		return nil, err
	}
	if _, err := decoder.Token(); err != io.EOF {
		return nil, errors.New("trailing JSON")
	}
	return value, nil
}

func parseV4Value(decoder *json.Decoder, depth int) (any, error) {
	if depth > maxV4NestingDepth {
		return nil, errors.New("JSON nesting limit")
	}
	token, err := decoder.Token()
	if err != nil {
		return nil, err
	}
	delim, isDelim := token.(json.Delim)
	if !isDelim {
		return token, nil
	}
	switch delim {
	case '{':
		object := make(map[string]any)
		for decoder.More() {
			if len(object) >= maxV4ObjectFields {
				return nil, errors.New("JSON object field limit")
			}
			keyToken, err := decoder.Token()
			if err != nil {
				return nil, err
			}
			key, ok := keyToken.(string)
			if !ok {
				return nil, errors.New("invalid JSON object key")
			}
			if _, exists := object[key]; exists {
				return nil, errors.New("duplicate JSON key")
			}
			value, err := parseV4Value(decoder, depth+1)
			if err != nil {
				return nil, err
			}
			object[key] = value
		}
		_, err := decoder.Token()
		return object, err
	case '[':
		array := make([]any, 0)
		for decoder.More() {
			if len(array) >= maxV4ArrayElements {
				return nil, errors.New("JSON array element limit")
			}
			value, err := parseV4Value(decoder, depth+1)
			if err != nil {
				return nil, err
			}
			array = append(array, value)
		}
		_, err := decoder.Token()
		return array, err
	default:
		return nil, errors.New("unexpected JSON delimiter")
	}
}

func hasExactV4Fields(object map[string]any, fields ...string) bool {
	if len(object) != len(fields) {
		return false
	}
	for _, field := range fields {
		if _, ok := object[field]; !ok {
			return false
		}
	}
	return true
}

func isV4ASCII(value string) bool {
	if len(value) == 0 { return false }
	for _, char := range value {
		if char < 0x21 || char > 0x7e {
			return false
		}
	}
	return true
}

func isV4Method(method string) bool {
	switch method {
	case string(LibXrayMethodConvertShareLinksToXrayJson),
		string(LibXrayMethodTestXray),
		string(LibXrayMethodRunXray),
		string(LibXrayMethodStopXray):
		return true
	default:
		return false
	}
}

func validateV4Payload(method string, payload map[string]any) error {
	switch method {
	case string(LibXrayMethodConvertShareLinksToXrayJson):
		if len(payload) < 1 || len(payload) > 2 {
			return errors.New("invalid convert payload")
		}
		if !onlyV4Fields(payload, "text", "age") {
			return errors.New("unknown convert payload field")
		}
		text, ok := payload["text"].(string)
		if !ok {
			return errors.New("invalid profile text")
		}
		if len(text) > maxV4ProfileBytes {
			return errV4RequestTooLarge
		}
		if age, exists := payload["age"]; exists {
			ageObject, ok := age.(map[string]any)
			if !ok || !hasExactV4Fields(ageObject, "secretKey") {
				return errors.New("invalid age payload")
			}
			secret, ok := ageObject["secretKey"].(string)
			if !ok {
				return errors.New("invalid age secret")
			}
			if len(secret) > maxV4ControlBytes {
				return errV4RequestTooLarge
			}
		}
	case string(LibXrayMethodTestXray), string(LibXrayMethodRunXray):
		if !hasExactV4Fields(payload, "xrayJson") {
			return errors.New("invalid config payload")
		}
		config, ok := payload["xrayJson"].(string)
		if !ok || len(config) == 0 || !utf8.ValidString(config) {
			return errors.New("invalid config")
		}
		if len(config) > maxV4ProfileBytes {
			return errV4RequestTooLarge
		}
	case string(LibXrayMethodStopXray):
		if len(payload) != 0 {
			return errors.New("stop payload must be empty")
		}
	default:
		return errors.New("unsupported method")
	}
	return nil
}

func onlyV4Fields(object map[string]any, fields ...string) bool {
	allowed := make(map[string]struct{}, len(fields))
	for _, field := range fields {
		allowed[field] = struct{}{}
	}
	for field := range object {
		if _, ok := allowed[field]; !ok {
			return false
		}
	}
	return true
}

func executeV4Method(method string, payload map[string]any) (any, error) {
	switch method {
	case string(LibXrayMethodConvertShareLinksToXrayJson):
		request := ConvertShareLinksToXrayJsonRequest{Text: payload["text"].(string)}
		if ageValue, exists := payload["age"]; exists {
			age := ageValue.(map[string]any)
			request.Age = &AgeDecryptConfig{SecretKey: age["secretKey"].(string)}
		}
		return share.ConvertShareLinksToXrayJson(request.Text, func() string {
			if request.Age == nil { return "" }
			return request.Age.SecretKey
		}())
	case string(LibXrayMethodTestXray):
		return struct{}{}, xray.TestXray(payload["xrayJson"].(string))
	case string(LibXrayMethodRunXray):
		if err := xray.RunXray(payload["xrayJson"].(string)); err != nil {
			return nil, err
		}
		return map[string]bool{"started": true}, nil
	case string(LibXrayMethodStopXray):
		if !xray.GetXrayState() {
			return map[string]bool{"stopped": true}, nil
		}
		if err := xray.StopXray(); err != nil {
			return nil, err
		}
		return map[string]bool{"stopped": true}, nil
	default:
		return nil, errors.New("unsupported method")
	}
}

func validateV4ResultPreflight(method string, data any) error {
	if method != string(LibXrayMethodConvertShareLinksToXrayJson) {
		return nil
	}
	value := reflect.ValueOf(data)
	for value.IsValid() && (value.Kind() == reflect.Pointer || value.Kind() == reflect.Interface) {
		if value.IsNil() { return errors.New("nil converted profile") }
		value = value.Elem()
	}
	if !value.IsValid() || value.Kind() != reflect.Struct {
		return errors.New("converted profile is not a struct")
	}
	typeInfo := value.Type()
	for i := 0; i < value.NumField(); i++ {
		field := typeInfo.Field(i)
		jsonName := strings.Split(field.Tag.Get("json"), ",")[0]
		if jsonName != "outbounds" { continue }
		outbounds := value.Field(i)
		for outbounds.IsValid() && (outbounds.Kind() == reflect.Pointer || outbounds.Kind() == reflect.Interface) {
			if outbounds.IsNil() { return errors.New("nil outbounds") }
			outbounds = outbounds.Elem()
		}
		if !outbounds.IsValid() || (outbounds.Kind() != reflect.Slice && outbounds.Kind() != reflect.Array) {
			return errors.New("outbounds is not an array")
		}
		if outbounds.Len() == 0 || outbounds.Len() > maxV4ConvertedOutbounds {
			return errors.New("outbound count limit")
		}
		return nil
	}
	return errors.New("outbounds field missing")
}

func validateV4Result(method string, dataJSON []byte) error {
	switch method {
	case string(LibXrayMethodConvertShareLinksToXrayJson):
		var root map[string]json.RawMessage
		if err := json.Unmarshal(dataJSON, &root); err != nil || len(root) != 1 {
			return errors.New("invalid converted profile")
		}
		var outbounds []json.RawMessage
		if err := json.Unmarshal(root["outbounds"], &outbounds); err != nil || len(outbounds) == 0 || len(outbounds) > maxV4ConvertedOutbounds {
			return errors.New("invalid converted outbounds")
		}
	case string(LibXrayMethodTestXray):
		if string(dataJSON) != "{}" {
			return errors.New("invalid test result")
		}
	case string(LibXrayMethodRunXray):
		var result map[string]bool
		if err := json.Unmarshal(dataJSON, &result); err != nil || len(result) != 1 || !result["started"] {
			return errors.New("invalid start result")
		}
	case string(LibXrayMethodStopXray):
		var result map[string]bool
		if err := json.Unmarshal(dataJSON, &result); err != nil || len(result) != 1 || !result["stopped"] {
			return errors.New("invalid stop result")
		}
	}
	return nil
}

func v4MethodError(method string) (string, string) {
	switch method {
	case string(LibXrayMethodConvertShareLinksToXrayJson), string(LibXrayMethodTestXray):
		return "INVALID_CONFIG", "VALIDATION"
	case string(LibXrayMethodRunXray):
		return "CORE_START_FAILED", "ENGINE"
	case string(LibXrayMethodStopXray):
		return "CORE_STOP_FAILED", "ENGINE"
	default:
		return "INTERNAL_ERROR", "INTERNAL"
	}
}

func encodeV4Failure(requestID, method *string, code, category string, retryable bool) string {
	response := v4Response{
		APIVersion: libXrayV4APIVersion,
		RequestID: requestID,
		Method: method,
		Success: false,
		Code: code,
		Data: nil,
		Error: &v4Error{Category: category, Retryable: retryable},
	}
	raw, err := json.Marshal(response)
	if err != nil || len(raw) > maxV4ControlBytes {
		return `{"apiVersion":4,"requestId":null,"method":null,"success":false,"code":"INTERNAL_ERROR","data":null,"error":{"category":"INTERNAL","retryable":false}}`
	}
	return string(raw)
}

// estimateV4JSONSize deliberately overestimates JSON escaping and stops once
// the method-specific budget is exceeded, before json.Marshal allocates output.
func estimateV4JSONSize(value reflect.Value, limit int) int {
	if !value.IsValid() {
		return 4
	}
	if value.Kind() == reflect.Interface || value.Kind() == reflect.Pointer {
		if value.IsNil() { return 4 }
		return estimateV4JSONSize(value.Elem(), limit)
	}
	switch value.Kind() {
	case reflect.String:
		return 2 + value.Len()*6
	case reflect.Bool:
		if value.Bool() { return 4 }
		return 5
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64,
		reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Float32, reflect.Float64:
		return 32
	case reflect.Slice, reflect.Array:
		total := 2
		for i := 0; i < value.Len(); i++ {
			total += 1 + estimateV4JSONSize(value.Index(i), limit)
			if total > limit { return total }
		}
		return total
	case reflect.Map:
		if value.IsNil() { return 4 }
		total := 2
		iter := value.MapRange()
		for iter.Next() {
			key := iter.Key()
			total += 1 + estimateV4JSONSize(key, limit) + 1 + estimateV4JSONSize(iter.Value(), limit)
			if total > limit { return total }
		}
		return total
	case reflect.Struct:
		total := 2
		typeInfo := value.Type()
		for i := 0; i < value.NumField(); i++ {
			field := typeInfo.Field(i)
			if field.PkgPath != "" { continue }
			tag := strings.Split(field.Tag.Get("json"), ",")[0]
			if tag == "-" { continue }
			if tag == "" { tag = field.Name }
			if strings.Contains(field.Tag.Get("json"), "omitempty") && value.Field(i).IsZero() { continue }
			total += 1 + len(tag)*6 + 3 + estimateV4JSONSize(value.Field(i), limit)
			if total > limit { return total }
		}
		return total
	default:
		return limit + 1
	}
}
