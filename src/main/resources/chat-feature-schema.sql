CREATE TABLE product_chat_rooms (
    room_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_message VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (room_id),
    CONSTRAINT uk_product_chat_room_product UNIQUE (product_id),
    CONSTRAINT fk_product_chat_room_product FOREIGN KEY (product_id) REFERENCES products (product_id)
);

CREATE INDEX idx_product_chat_room_product ON product_chat_rooms (product_id);
CREATE INDEX idx_product_chat_room_status ON product_chat_rooms (status);

CREATE TABLE product_chat_messages (
    message_id BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_deleted BIT NOT NULL,
    report_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (message_id),
    CONSTRAINT fk_product_chat_message_room FOREIGN KEY (room_id) REFERENCES product_chat_rooms (room_id),
    CONSTRAINT fk_product_chat_message_sender FOREIGN KEY (sender_id) REFERENCES members (member_id)
);

CREATE INDEX idx_product_chat_message_room ON product_chat_messages (room_id);
CREATE INDEX idx_product_chat_message_created ON product_chat_messages (created_at);
