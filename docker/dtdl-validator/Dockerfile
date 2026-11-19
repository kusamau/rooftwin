# DTDL v3 validator: Microsoft's DTDLParser (C#) compiled to WebAssembly, run on Node.
#
#   docker build -t dtdl-validator docker/dtdl-validator
#   docker run --rm -v "$PWD/models/dtdl:/w:ro" dtdl-validator /w
#
# Exit code 0 = valid. Nothing is installed on the host.
FROM node:26-alpine

WORKDIR /opt/validator
COPY package.json package-lock.json ./
RUN npm ci --omit=dev --no-audit --no-fund && npm cache clean --force
COPY validate.mjs ./

USER node
ENTRYPOINT ["node", "/opt/validator/validate.mjs"]
CMD ["/w"]
