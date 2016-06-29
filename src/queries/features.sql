-- name: insert-feature!
-- Inserts a new feature into the table.
INSERT INTO features(
  genomeName, geneName, condition, numTASites, geneLength, numControlReads, numExperimentReads, modifiedRatio, p, essentialityIndex, fitness)
VALUES (
  :genome_name, :gene_name, :condition, :num_TA_sites, :gene_length, :num_control_reads, :num_experiment_reads, :modified_ratio, :p, :essentiality_index, :fitness);

-- name: select-features
-- Selects all feature records for a given gene.
SELECT * FROM features WHERE genomeName = :genome_name AND geneName = :gene_name;

-- name: delete-features!
-- Deletes all feature records for a given condition.
DELETE * FROM features WHERE condition = :condition;
