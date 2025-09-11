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

type VSPTBand struct {
	Locale string
	Level string
	Low int
	High int
}

type VSPTWord struct {
	WordId string
	Word string
	Valid int32
	Weight int32
}

type SAGrade struct {
	Skill string
	Rsc int
	Ppe float64
	Se float64
	Grade int
}

type PreestWeight struct {
	Sa float64
	Vspt float64
	Coe float64
}

type PreestAssignment struct {
	Pe float64
	BookletId int
}

type Session struct {
	Al       string `json:"al"`
	Tl       string `json:"tl"`
	Skill    string `json:"skill"`
	VsptDone struct {
		FraFr bool `json:"fra_fr"`
	} `json:"vsptDone"`
	ReviewBasket   any `json:"reviewBasket"`
	ReviewItemID   any `json:"reviewItemId"`
	FeedbackMode   bool        `json:"feedbackMode"`
	TestDone       bool        `json:"testDone"`
	TestDifficulty string      `json:"testDifficulty"`
	Id             string      `json:"id"`
	VsptSubmitted  int         `json:"vsptSubmitted"`
	VsptMearaScore float64     `json:"vsptMearaScore"`
	VsptZScore     float64     `json:"vsptZScore"`
	VsptLevel      string      `json:"vsptLevel"`
	SaSubmitted    int         `json:"saSubmitted"`
	SaPPE          float64     `json:"saPPE"`
	SaLevel        int         `json:"saLevel"`
	SaDone         bool        `json:"saDone"`
	BookletId	   int		   `json:"bookletId"`
}
