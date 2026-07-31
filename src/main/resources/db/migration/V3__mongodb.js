// ============================================================
// FinDART PoC - MongoDB initialization
//
// MongoDB owns:
//   - contents: original documents, normalized body, analysis, LLM metadata
//   - summaries: intraday/day/week/month/quarter/half-year/year summaries
//
// Run:
//   mongosh "mongodb://<user>:<password>@<host>:27017/findart?authSource=findart" \
//     /path/to/findart_mongodb_init.js
//
// Notes:
//   - Application-generated UUID strings are used as _id values.
//   - MariaDB content_references.content_id and summary_references.summary_id
//     must use the same UUID strings.
// ============================================================

const targetDb = db.getSiblingDB("findart");

function collectionExists(name) {
  return targetDb.getCollectionInfos({ name }).length > 0;
}

// ------------------------------------------------------------
// 1. contents
// ------------------------------------------------------------
if (!collectionExists("contents")) {
  targetDb.createCollection("contents", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: [
          "_id",
          "identity",
          "title",
          "publishedAt",
          "collectedAt",
          "processing",
          "createdAt",
          "updatedAt"
        ],
        properties: {
          _id: {
            bsonType: "string",
            description: "Application-generated UUID string"
          },
          identity: {
            bsonType: "object",
            required: [
              "contentType",
              "source",
              "externalId",
              "revision",
              "checksum"
            ],
            properties: {
              contentType: { bsonType: "string" },
              source: { bsonType: "string" },
              externalId: { bsonType: "string" },
              revision: { bsonType: ["int", "long"] },
              checksum: { bsonType: "string" }
            }
          },
          title: { bsonType: "string" },
          sourceUrl: { bsonType: ["string", "null"] },
          publisher: { bsonType: ["string", "null"] },
          language: { bsonType: ["string", "null"] },

          publishedAt: { bsonType: "date" },
          collectedAt: { bsonType: "date" },
          effectiveDate: { bsonType: ["date", "null"] },

          body: {
            bsonType: ["object", "null"],
            properties: {
              raw: { bsonType: ["string", "null"] },
              normalized: { bsonType: ["string", "null"] },
              rawTokenCount: { bsonType: ["int", "long", "null"] },
              normalizedTokenCount: { bsonType: ["int", "long", "null"] },
              storageType: { bsonType: ["string", "null"] },
              storageUri: { bsonType: ["string", "null"] }
            }
          },

          analysis: {
            bsonType: ["object", "null"],
            properties: {
              shortSummary: { bsonType: ["string", "null"] },
              longSummary: { bsonType: ["string", "null"] },
              categories: { bsonType: ["array", "null"] },
              companies: { bsonType: ["array", "null"] },
              sectors: { bsonType: ["array", "null"] },
              keywords: { bsonType: ["array", "null"] },
              importanceScore: {
                bsonType: ["double", "decimal", "int", "long", "null"]
              },
              marketImpactScore: {
                bsonType: ["double", "decimal", "int", "long", "null"]
              }
            }
          },

          processing: {
            bsonType: "object",
            required: ["status"],
            properties: {
              status: { bsonType: "string" },
              modelProvider: { bsonType: ["string", "null"] },
              modelName: { bsonType: ["string", "null"] },
              promptVersion: { bsonType: ["string", "null"] },
              pipelineVersion: { bsonType: ["string", "null"] },
              inputTokens: { bsonType: ["int", "long", "null"] },
              outputTokens: { bsonType: ["int", "long", "null"] },
              errorMessage: { bsonType: ["string", "null"] }
            }
          },

          attributes: { bsonType: ["object", "null"] },
          current: { bsonType: ["bool", "null"] },
          createdAt: { bsonType: "date" },
          updatedAt: { bsonType: "date" }
        }
      }
    },
    validationLevel: "moderate",
    validationAction: "error"
  });
}

// A source revision must be unique.
targetDb.contents.createIndex(
  {
    "identity.contentType": 1,
    "identity.source": 1,
    "identity.externalId": 1,
    "identity.revision": 1
  },
  {
    name: "uk_contents_source_revision",
    unique: true
  }
);

targetDb.contents.createIndex(
  {
    "identity.contentType": 1,
    publishedAt: -1
  },
  { name: "idx_contents_type_published" }
);

targetDb.contents.createIndex(
  {
    "processing.status": 1,
    collectedAt: 1
  },
  { name: "idx_contents_processing_status" }
);

targetDb.contents.createIndex(
  {
    "analysis.companies.companyId": 1,
    publishedAt: -1
  },
  { name: "idx_contents_company_published" }
);

targetDb.contents.createIndex(
  {
    "analysis.sectors.sectorId": 1,
    publishedAt: -1
  },
  { name: "idx_contents_sector_published" }
);

targetDb.contents.createIndex(
  {
    "analysis.categories": 1,
    publishedAt: -1
  },
  { name: "idx_contents_category_published" }
);

// ------------------------------------------------------------
// 2. summaries
// ------------------------------------------------------------
if (!collectionExists("summaries")) {
  targetDb.createCollection("summaries", {
    validator: {
      $jsonSchema: {
        bsonType: "object",
        required: [
          "_id",
          "summaryType",
          "scope",
          "period",
          "summaryText",
          "revision",
          "current",
          "createdAt",
          "updatedAt"
        ],
        properties: {
          _id: {
            bsonType: "string",
            description: "Application-generated UUID string"
          },
          summaryType: { bsonType: "string" },

          scope: {
            bsonType: "object",
            required: ["type", "key"],
            properties: {
              type: { bsonType: "string" },
              key: { bsonType: "string" },
              companyId: { bsonType: ["string", "null"] },
              sectorId: { bsonType: ["long", "int", "null"] },
              issueId: { bsonType: ["string", "null"] },
              categoryCode: { bsonType: ["string", "null"] },
              economicDomain: { bsonType: ["string", "null"] }
            }
          },

          period: {
            bsonType: "object",
            required: ["type", "windowCode", "start", "end"],
            properties: {
              type: { bsonType: "string" },
              windowCode: { bsonType: "string" },
              start: { bsonType: "date" },
              end: { bsonType: "date" }
            }
          },

          title: { bsonType: ["string", "null"] },
          summaryText: { bsonType: "string" },
          payload: { bsonType: ["object", "null"] },

          sources: {
            bsonType: ["object", "null"],
            properties: {
              contentIds: { bsonType: ["array", "null"] },
              summaryIds: { bsonType: ["array", "null"] },
              metricCodes: { bsonType: ["array", "null"] }
            }
          },

          processing: {
            bsonType: ["object", "null"],
            properties: {
              modelProvider: { bsonType: ["string", "null"] },
              modelName: { bsonType: ["string", "null"] },
              promptVersion: { bsonType: ["string", "null"] },
              pipelineVersion: { bsonType: ["string", "null"] },
              inputTokens: { bsonType: ["int", "long", "null"] },
              outputTokens: { bsonType: ["int", "long", "null"] }
            }
          },

          revision: { bsonType: ["int", "long"] },
          current: { bsonType: "bool" },
          createdAt: { bsonType: "date" },
          updatedAt: { bsonType: "date" }
        }
      }
    },
    validationLevel: "moderate",
    validationAction: "error"
  });
}

targetDb.summaries.createIndex(
  {
    summaryType: 1,
    "scope.key": 1,
    "period.type": 1,
    "period.windowCode": 1,
    "period.start": 1,
    "period.end": 1,
    revision: 1
  },
  {
    name: "uk_summaries_scope_period_revision",
    unique: true
  }
);

targetDb.summaries.createIndex(
  {
    summaryType: 1,
    "scope.key": 1,
    "period.type": 1,
    "period.end": -1,
    current: 1
  },
  { name: "idx_summaries_current_lookup" }
);

targetDb.summaries.createIndex(
  {
    "scope.issueId": 1,
    "period.end": -1,
    current: 1
  },
  { name: "idx_summaries_issue" }
);

targetDb.summaries.createIndex(
  {
    "scope.companyId": 1,
    "period.end": -1,
    current: 1
  },
  { name: "idx_summaries_company" }
);

targetDb.summaries.createIndex(
  {
    "scope.sectorId": 1,
    "period.end": -1,
    current: 1
  },
  { name: "idx_summaries_sector" }
);

// ------------------------------------------------------------
// Verification
// ------------------------------------------------------------
print("FinDART MongoDB initialization complete.");
printjson({
  database: targetDb.getName(),
  collections: targetDb.getCollectionNames(),
  contentsIndexes: targetDb.contents.getIndexes().map(index => index.name),
  summariesIndexes: targetDb.summaries.getIndexes().map(index => index.name)
});