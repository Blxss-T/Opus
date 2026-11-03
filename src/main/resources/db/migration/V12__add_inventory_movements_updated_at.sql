-- InventoryMovement extends BaseEntity, which maps updated_at, but V7 created
-- the table without it. Hibernate schema validation therefore failed on any
-- database built purely from migrations.
--
-- IF NOT EXISTS keeps this safe for databases where the column was already
-- added by hand to work around the startup failure.
ALTER TABLE inventory_movements
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;
