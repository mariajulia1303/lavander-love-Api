-- uns dados iniciais só pra API não começar vazia

INSERT INTO categoria (nome, descricao) VALUES ('Buques', 'Buques variados pra qualquer ocasiao');
INSERT INTO categoria (nome, descricao) VALUES ('Plantas', 'Plantas ornamentais e suculentas');
INSERT INTO categoria (nome, descricao) VALUES ('Vasos', 'Vasos de ceramica e plastico');

INSERT INTO produto (nome, preco, estoque, categoria_id) VALUES ('Buque de Rosas Vermelhas', 89.90, 15, 1);
INSERT INTO produto (nome, preco, estoque, categoria_id) VALUES ('Suculenta Echeveria', 19.90, 40, 2);
INSERT INTO produto (nome, preco, estoque, categoria_id) VALUES ('Vaso de Ceramica Branco', 34.50, 25, 3);

INSERT INTO endereco (logradouro, numero, bairro, cidade, uf, cep) VALUES ('Rua das Flores', '100', 'Centro', 'Sao Paulo', 'SP', '01001000');

INSERT INTO cliente (nome, email, telefone, endereco_id) VALUES ('Maria Silva', 'maria.silva@email.com', '11999990000', 1);
