{{/*
Chart 이름
*/}}
{{- define "findart-backend.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
릴리스 전체 이름
*/}}
{{- define "findart-backend.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name (include "findart-backend.name" .) | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
공통 라벨
*/}}
{{- define "findart-backend.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
app.kubernetes.io/name: {{ include "findart-backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Pod selector 라벨
*/}}
{{- define "findart-backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "findart-backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

