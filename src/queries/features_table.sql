CREATE TABLE IF NOT EXISTS features (
  genome_name TEXT,
  gene_name TEXT,
  condition TEXT,
  num_ta_sites INTEGER,
  gene_length INTEGER,
  num_control_reads REAL,
  num_experiment_reads REAL,
  modified_ratio REAL,
  p REAL,
  essentiality_index REAL,
  fitness REAL
);
