CREATE TABLE patient (
    patient_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    dob DATE NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    blood_group VARCHAR(5) NOT NULL,
    address TEXT
);

CREATE TABLE doctor (
    doctor_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    consultation_fee NUMERIC(10, 2) NOT NULL
);

CREATE TABLE appointment (
    appointment_id SERIAL PRIMARY KEY,
    patient_id INT REFERENCES patient(patient_id),
    doctor_id INT REFERENCES doctor(doctor_id),
    appointment_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending'
);

CREATE TABLE consultation (
    consultation_id SERIAL PRIMARY KEY,
    appointment_id INT REFERENCES appointment(appointment_id),
    diagnosis TEXT NOT NULL,
    notes TEXT,
    follow_up_date DATE
);

CREATE TABLE bill (
    bill_id SERIAL PRIMARY KEY,
    consultation_id INT REFERENCES consultation(consultation_id),
    patient_id INT REFERENCES patient(patient_id),
    total_amount NUMERIC(10, 2) NOT NULL,
    paid_amount NUMERIC(10, 2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'Pending'
);

CREATE TABLE payment (
    payment_id SERIAL PRIMARY KEY,
    bill_id INT REFERENCES bill(bill_id),
    amount NUMERIC(10, 2) NOT NULL,
    payment_mode VARCHAR(20) NOT NULL,
    payment_date DATE DEFAULT CURRENT_DATE
);

INSERT INTO doctor (name, specialization, consultation_fee) VALUES
('Dr. Ramesh Sharma', 'Cardiology', 800.00),
('Dr. Anita Roy', 'Pediatrics', 500.00),
('Dr. Vikram Patel', 'Orthopedics', 750.00);