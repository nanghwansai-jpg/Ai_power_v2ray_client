package share

import (
	"encoding/json"
	"reflect"
	"strings"
	"testing"

	"github.com/xtls/xray-core/infra/conf"
)

func TestV4ShareSizeEstimatorBoundsRawMessage(t *testing.T) {
	raw := json.RawMessage(`{"credential":"` + strings.Repeat("x", 4096) + `"}`)
	if got := estimateShareJSONSize(reflect.ValueOf(raw), 1024); got <= 1024 {
		t.Fatalf("RawMessage preflight size = %d, want > 1024", got)
	}
}

func TestV4ShareRejectsTooManyOutboundsBeforeMarshal(t *testing.T) {
	config := &conf.Config{OutboundConfigs: make([]conf.OutboundDetourConfig, maxV4ShareOutbounds+1)}
	if _, err := filterBuildableOutboundsV4(config); err == nil {
		t.Fatal("V4 conversion accepted too many outbounds")
	}
}

func TestV4ShareRejectsOversizedRawOutboundBeforeMarshal(t *testing.T) {
	raw := json.RawMessage(`"` + strings.Repeat("x", maxV4ShareOutputBytes) + `"`)
	config := &conf.Config{
		OutboundConfigs: []conf.OutboundDetourConfig{{Settings: &raw}},
	}
	if _, err := filterBuildableOutboundsV4(config); err == nil {
		t.Fatal("V4 conversion accepted oversized raw outbound settings")
	}
}

func TestV4ShareRejectsOversizedPlaintextInput(t *testing.T) {
	input := strings.Repeat("x", maxV4ShareInputBytes+1)
	if _, _, err := decryptShareTextV4(input, ""); err == nil {
		t.Fatal("V4 conversion accepted oversized plaintext input")
	}
}
