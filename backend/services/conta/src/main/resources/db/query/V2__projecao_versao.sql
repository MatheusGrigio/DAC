ALTER TABLE projecao_aplicada ADD COLUMN numero_conta VARCHAR(4) NOT NULL DEFAULT '';
ALTER TABLE projecao_aplicada ADD COLUMN versao INT NOT NULL DEFAULT 0;

ALTER TABLE projecao_aplicada ALTER COLUMN numero_conta DROP DEFAULT;
ALTER TABLE projecao_aplicada ALTER COLUMN versao DROP DEFAULT;

CREATE UNIQUE INDEX projecao_conta_versao_uk
    ON projecao_aplicada (numero_conta, versao)
    WHERE numero_conta <> '';
