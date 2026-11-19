# AAS validator: metamodel V3.0 and V3.1 checks (aas-core-works SDKs) plus referential integrity.
#
#   docker build -t aas-validator docker/aas-validator
#   docker run --rm -v "$PWD/models/aas:/w:ro" aas-validator /w/Solar.json
FROM python:3.13-slim

WORKDIR /opt/validator
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY validate_aas.py ./

USER nobody
ENTRYPOINT ["python3", "-I", "/opt/validator/validate_aas.py"]
CMD ["/w/Solar.json"]
