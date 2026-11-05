CREATE TABLE Department (
    department_id SERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE Doctor (
    doctor_id SERIAL PRIMARY KEY,
    department_id INTEGER NOT NULL,
    doctor_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE,
    email VARCHAR(150) UNIQUE,
    consultation_fee NUMERIC(10,2) NOT NULL CHECK (consultation_fee >= 0),
    FOREIGN KEY (department_id)
        REFERENCES Department(department_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE Patient (
    patient_id SERIAL PRIMARY KEY,
    patient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    gender CHAR(1) NOT NULL CHECK (gender IN ('M', 'F', 'O')),
    date_of_birth DATE NOT NULL CHECK (date_of_birth <= CURRENT_DATE),
    address TEXT
);

CREATE TABLE Appointment (
    appointment_id SERIAL PRIMARY KEY,
    patient_id INTEGER NOT NULL,
    doctor_id INTEGER NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Scheduled'
        CHECK (status IN ('Scheduled', 'Completed', 'Cancelled', 'No Show')),
    reason TEXT,
    UNIQUE (doctor_id, appointment_date, appointment_time),
    FOREIGN KEY (patient_id)
        REFERENCES Patient(patient_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (doctor_id)
        REFERENCES Doctor(doctor_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE Consultation (
    consultation_id SERIAL PRIMARY KEY,
    appointment_id INTEGER NOT NULL UNIQUE,
    consultation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diagnosis TEXT NOT NULL,
    notes TEXT,
    FOREIGN KEY (appointment_id)
        REFERENCES Appointment(appointment_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE Medicine (
    medicine_id SERIAL PRIMARY KEY,
    medicine_name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    unit_price NUMERIC(10,2) NOT NULL CHECK (unit_price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0)
);

CREATE TABLE Prescription (
    prescription_id SERIAL PRIMARY KEY,
    consultation_id INTEGER NOT NULL,
    medicine_id INTEGER NOT NULL,
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    instructions TEXT,
    UNIQUE (consultation_id, medicine_id),
    FOREIGN KEY (consultation_id)
        REFERENCES Consultation(consultation_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (medicine_id)
        REFERENCES Medicine(medicine_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE Bill (
    bill_id SERIAL PRIMARY KEY,
    consultation_id INTEGER NOT NULL UNIQUE,
    consultation_fee NUMERIC(10,2) NOT NULL CHECK (consultation_fee >= 0),
    medicine_charges NUMERIC(10,2) NOT NULL CHECK (medicine_charges >= 0),
    total_amount NUMERIC(10,2) NOT NULL,
    bill_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending'
        CHECK (status IN ('Pending', 'Partially Paid', 'Paid')),
    CHECK (total_amount = consultation_fee + medicine_charges),
    FOREIGN KEY (consultation_id)
        REFERENCES Consultation(consultation_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

CREATE TABLE Payment (
    payment_id SERIAL PRIMARY KEY,
    bill_id INTEGER NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_method VARCHAR(20) NOT NULL
        CHECK (payment_method IN ('Cash', 'Card', 'UPI', 'Net Banking')),
    transaction_reference VARCHAR(100) UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'Successful'
        CHECK (status IN ('Successful', 'Failed', 'Refunded')),
    FOREIGN KEY (bill_id)
        REFERENCES Bill(bill_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

INSERT INTO Department (department_name, description) VALUES
('Cardiology', 'Heart and cardiovascular care'),
('Neurology', 'Brain and nervous system care'),
('Orthopedics', 'Bone and joint care'),
('General Medicine', 'General medical consultation'),
('Dermatology', 'Skin and hair care');

INSERT INTO Doctor
(department_id, doctor_name, specialization, phone, email, consultation_fee)
VALUES
(1, 'Dr. Arun Kumar', 'Cardiologist', '9000000001', 'arun@hospital.com', 700.00),
(2, 'Dr. Priya Sharma', 'Neurologist', '9000000002', 'priya@hospital.com', 800.00),
(3, 'Dr. Ravi Kumar', 'Orthopedic Surgeon', '9000000003', 'ravi@hospital.com', 600.00),
(4, 'Dr. Meena Devi', 'General Physician', '9000000004', 'meena@hospital.com', 400.00),
(5, 'Dr. Karthik Raj', 'Dermatologist', '9000000005', 'karthik@hospital.com', 500.00);

INSERT INTO Medicine
(medicine_name, description, unit_price, stock_quantity)
VALUES
('Paracetamol', 'Used for fever and mild pain', 2.50, 500),
('Amoxicillin', 'Antibiotic medicine', 8.00, 300),
('Cetirizine', 'Used for allergy relief', 3.00, 250),
('Omeprazole', 'Used to reduce stomach acid', 5.00, 200),
('Ibuprofen', 'Used for pain and inflammation', 4.50, 350);
