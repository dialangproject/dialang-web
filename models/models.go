package models

type SetTLParams struct {
	SessionId string `json:"sessionId"`
	PassId string `json:"passId"`
	Al string `json:"al"`
	Tl string `json:"tl"`
	Skill string `json:"skill"`
	IPAddress string
	BrowserLocale string
	Referrer string
}

