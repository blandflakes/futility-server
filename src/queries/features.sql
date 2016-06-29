-- name: insert-feature!
-- Inserts a new feature into the table.
INSERT INTO features(
  genome_name, gene_name, condition, num_ta_sites, gene_length, num_control_reads, num_experiment_reads, modified_ratio, p, essentiality_index, fitness)
VALUES (
  :genome_name, :gene_name, :condition, :num_ta_sites, :gene_length, :num_control_reads, :num_experiment_reads, :modified_ratio, :p, :essentiality_index, :fitness);

-- name: select-features
-- Selects all feature records for a given gene.
SELECT condition, num_ta_sites, gene_length, num_control_reads, num_experiment_reads, modified_ratio, p, essentiality_index, fitness
FROM features WHERE genome_name = :genome_name AND gene_name = :gene_name;

-- name: delete-features!
-- Deletes all feature records for a given condition.
DELETE FROM features WHERE condition = :condition;
