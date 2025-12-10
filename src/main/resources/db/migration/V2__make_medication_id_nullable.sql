-- Migration to make medication_id nullable in dose_logs table
-- This allows doses to be created with only user_medication_id reference
-- Required for the new UserMedication-based medication management

ALTER TABLE dose_logs ALTER COLUMN medication_id DROP NOT NULL;
