CREATE TABLE IF NOT EXISTS features (
  genomeName TEXT,
  geneName TEXT,
  condition TEXT,
  numTASites INTEGER,
  geneLength INTEGER,
  numControlReads REAL,
  numExperimentReads REAL,
  modifiedRatio REAL,
  p REAL,
  essentialityIndex REAL,
  fitness REAL
);
