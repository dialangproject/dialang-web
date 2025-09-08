FROM golang:1.25

WORKDIR /app

# pre-copy/cache go.mod for pre-downloading dependencies and only redownloading them in subsequent builds if they change
COPY go.mod go.sum ./
RUN go mod download

COPY *.go ./
COPY handlers/ handlers/
COPY models/ models/
COPY utils/ utils/
COPY datacapture/ datacapture/
RUN CGO_ENABLED=0 GOOS=linux go build -o dialang-web
RUN chmod o+x dialang-web

CMD ["/app/dialang-web"]
