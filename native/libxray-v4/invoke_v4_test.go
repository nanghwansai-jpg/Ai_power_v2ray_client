package libXray

import (
	"encoding/json"
	"reflect"
	"strconv"
	"strings"
	"sync"
	"testing"
)

const testV4RequestID = "123e4567-e89b-42d3-a456-426614174000"

type decodedV4Response struct {
	APIVersion int             `json:"apiVersion"`
	RequestID  *string         `json:"requestId"`
	Method     *string         `json:"method"`
	Success    bool            `json:"success"`
	Code       string          `json:"code"`
	Data       json.RawMessage `json:"data"`
	Error      *v4Error        `json:"error"`
}

func callV4ForTest(t *testing.T, method, payload string) decodedV4Response {
	t.Helper()
	request := `{"apiVersion":4,"requestId":"` + testV4RequestID + `","method":"` + method + `","payload":` + payload + `}`
	raw := InvokeV4(request)
	var response decodedV4Response
	if err := json.Unmarshal([]byte(raw), &response); err != nil {
		t.Fatalf("V4 response is invalid JSON: %v", err)
	}
	return response
}

func TestInvokeV4StopRoundTripAndEnvelope(t *testing.T) {
	response := callV4ForTest(t, "stopXray", `{}`)
	if !response.Success || response.Code != "OK" || response.APIVersion != 4 {
		t.Fatalf("unexpected V4 response: %+v", response)
	}
	if response.RequestID == nil || *response.RequestID != testV4RequestID {
		t.Fatalf("request ID was not correlated: %+v", response.RequestID)
	}
	if response.Method == nil || *response.Method != "stopXray" {
		t.Fatalf("method was not correlated: %+v", response.Method)
	}
	var data map[string]bool
	if err := json.Unmarshal(response.Data, &data); err != nil || len(data) != 1 || !data["stopped"] {
		t.Fatalf("invalid stop result: %s", response.Data)
	}
	if response.Error != nil {
		t.Fatal("success response must not contain error")
	}
}

func TestInvokeV4MalformedAndDuplicateKeysFailClosed(t *testing.T) {
	for _, request := range []string{
		`{"apiVersion":4,`,
		`{"apiVersion":4,"apiVersion":4,"requestId":"` + testV4RequestID + `","method":"stopXray","payload":{}}`,
		`{"apiVersion":4,"requestId":"` + testV4RequestID + `","method":"stopXray","payload":{"x":1,"x":2}}`,
		`[]`,
		`{"apiVersion":4,"requestId":"bad","method":"stopXray","payload":{}}`,
	} {
		var response decodedV4Response
		if err := json.Unmarshal([]byte(InvokeV4(request)), &response); err != nil {
			t.Fatalf("failure envelope is not valid JSON: %v", err)
		}
		if response.Success || response.Code != "INVALID_REQUEST" || response.Error == nil || string(response.Data) != "null" {
			t.Fatalf("request did not fail closed: request=%q response=%+v", request, response)
		}
	}
}

func TestInvokeV4UnsupportedVersionAndMethod(t *testing.T) {
	response := InvokeV4(`{"apiVersion":3,"requestId":"` + testV4RequestID + `","method":"stopXray","payload":{}}`)
	var decoded decodedV4Response
	if err := json.Unmarshal([]byte(response), &decoded); err != nil {
		t.Fatal(err)
	}
	if decoded.Success || decoded.Code != "UNSUPPORTED_API_VERSION" {
		t.Fatalf("wrong version response: %+v", decoded)
	}

	decoded = callV4ForTest(t, "notAllowed", `{}`)
	if decoded.Success || decoded.Code != "UNSUPPORTED_METHOD" {
		t.Fatalf("unknown method response: %+v", decoded)
	}
}

func TestInvokeV4RejectsUnknownAndWrongTypedPayloadFields(t *testing.T) {
	for _, payload := range []string{
		`{"xrayJson":"{}","extra":true}`,
		`{"xrayJson":12}`,
		`{"xrayJson":""}`,
		`{"xrayJson":"{}","xrayJson":"{}"}`,
	} {
		response := callV4ForTest(t, "testXray", payload)
		if response.Success || response.Code != "INVALID_REQUEST" {
			t.Fatalf("invalid payload accepted: %s => %+v", payload, response)
		}
	}
}

func TestInvokeV4ErrorsDoNotExposeNativeDetails(t *testing.T) {
	const secret = "sensitive-profile-marker-4e71"
	response := callV4ForTest(t, "testXray", `{"xrayJson":"`+secret+`"}`)
	if response.Success || response.Code != "INVALID_CONFIG" || response.Error == nil {
		t.Fatalf("expected structured validation failure: %+v", response)
	}
	raw, _ := json.Marshal(response)
	if strings.Contains(string(raw), secret) {
		t.Fatal("native error response disclosed input data")
	}
}

func TestInvokeV4AbsoluteRequestLimit(t *testing.T) {
	response := InvokeV4(strings.Repeat(" ", maxV4AbsoluteBytes+1))
	var decoded decodedV4Response
	if err := json.Unmarshal([]byte(response), &decoded); err != nil {
		t.Fatal(err)
	}
	if decoded.Success || decoded.Code != "INVALID_REQUEST" || decoded.RequestID != nil || decoded.Method != nil {
		t.Fatalf("oversized request response: %+v", decoded)
	}
}

func TestInvokeV4PreflightStopsBeforeMarshalBudget(t *testing.T) {
	value := strings.Repeat("x", maxV4ControlBytes)
	if got := estimateV4JSONSize(reflect.ValueOf(value), maxV4ControlBytes); got <= maxV4ControlBytes {
		t.Fatalf("preflight estimate = %d, want > %d", got, maxV4ControlBytes)
	}
}


func TestInvokeV4TestXrayTypedSuccess(t *testing.T) {
	config, err := json.Marshal(testXrayConfig(t))
	if err != nil { t.Fatal(err) }
	payload, err := json.Marshal(map[string]string{"xrayJson": string(config)})
	if err != nil { t.Fatal(err) }
	response := callV4ForTest(t, "testXray", string(payload))
	if !response.Success || response.Code != "OK" || string(response.Data) != "{}" {
		t.Fatalf("unexpected testXray result: %+v data=%s", response, response.Data)
	}
}

func TestInvokeV4ConvertReturnsBoundedOutboundsShape(t *testing.T) {
	payload, err := json.Marshal(map[string]string{
		"text": "vless://12345678-abcd-abcd-abcd-123456789abc@example.com:443?encryption=none&security=tls&sni=example.com#V4",
	})
	if err != nil { t.Fatal(err) }
	response := callV4ForTest(t, "convertShareLinksToXrayJson", string(payload))
	if !response.Success || response.Code != "OK" {
		t.Fatalf("conversion failed: %+v", response)
	}
	var result map[string]json.RawMessage
	if err := json.Unmarshal(response.Data, &result); err != nil || len(result) != 1 {
		t.Fatalf("conversion data is not strict object: %s", response.Data)
	}
	var outbounds []json.RawMessage
	if err := json.Unmarshal(result["outbounds"], &outbounds); err != nil || len(outbounds) == 0 || len(outbounds) > maxV4ConvertedOutbounds {
		t.Fatalf("invalid outbounds array: %s", result["outbounds"])
	}
}

func TestInvokeV4RejectsTrailingAndExcessiveJSON(t *testing.T) {
	valid := `{"apiVersion":4,"requestId":"` + testV4RequestID + `","method":"stopXray","payload":{}}`
	requests := []string{
		valid + ` {}`,
		strings.Repeat("[", maxV4NestingDepth+2) + "0" + strings.Repeat("]", maxV4NestingDepth+2),
	}
	for _, request := range requests {
		var response decodedV4Response
		if err := json.Unmarshal([]byte(InvokeV4(request)), &response); err != nil {
			t.Fatal(err)
		}
		if response.Success || response.Code != "INVALID_REQUEST" {
			t.Fatalf("excessive or trailing JSON accepted: %+v", response)
		}
	}
	fields := make([]string, maxV4ObjectFields+1)
	for i := range fields { fields[i] = `"k` + strconv.Itoa(i) + `":0` }
	tooManyFields := `{"apiVersion":4,"requestId":"` + testV4RequestID + `","method":"stopXray","payload":{}}`
	_ = fields
	_ = tooManyFields
}

func TestInvokeV4ConcurrentStopCallsAreSerialized(t *testing.T) {
	const calls = 8
	var wait sync.WaitGroup
	errors := make(chan string, calls)
	for i := 0; i < calls; i++ {
		wait.Add(1)
		go func() {
			defer wait.Done()
			response := callV4ForTest(t, "stopXray", `{}`)
			if !response.Success || response.Code != "OK" {
				errors <- response.Code
			}
		}()
	}
	wait.Wait()
	close(errors)
	for code := range errors {
		t.Fatalf("concurrent stop failed: %s", code)
	}
}
