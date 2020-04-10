# Schema API CI/CD integration examples

This repository serves as a simple example of integrating Schema API into a typical CI/CD workflow. 

## Overview

In the [app](app) directory you can find a basic HTML/JS page embedding the [Snowplow Javascript Tracker](https://github.com/snowplow/snowplow-javascript-tracker),
while in the repo's root a file called [snowplow-schemas.json](snowplow-schemas.json) describes the schema dependencies for the app.  

Such file will be parsed, validated and used by Schema CI in a pipeline step to check that each schema is 
correctly deployed to the proper environment, before the code for the app gets deployed, effectively guarding against a major
source of Failed Events.

## Manifest validation
The [snowplow-schemas.json](snowplow-schemas.json) file, which is intended to be kept in sync with the source code by declaring and keeping updated the app's schema dependencies,
will be validated against the following [Self Describing JSON Schema](https://snowplowanalytics.com/blog/2014/05/15/introducing-self-describing-jsons/#sdjs): 

```yaml
{
  "$schema": "http://iglucentral.com/schemas/com.snowplowanalytics.self-desc/schema/jsonschema/1-0-0#",
  "description": "Schema for recording schema dependencies",
  "self": {
    "vendor": "com.snowplowanalytics.schemaci",
    "name": "schema_dependencies",
    "format": "jsonschema",
    "version": "1-0-0"
  },
  "type": "object",
  "properties": {
    "schemas": {
      "type": "array",
      "description": "An array of schema dependencies",
      "items": {
        "type": "object",
        "properties": {
          "vendor": {
            "type": "string",
            "pattern": "^[a-zA-Z0-9-_.]+$"
          },
          "name": {
            "type": "string",
            "pattern": "^[a-zA-Z0-9-_]+$"
          },
          "format": {
            "type": "string",
            "pattern": "^[a-zA-Z0-9-_]+$"
          },
          "version": {
            "type": "string",
            "pattern": "^[0-9]+-[0-9]+-[0-9]+$"
          }
        },
        "required": ["vendor", "name", "format", "version"],
        "additionalProperties": false
      }
    }
  },
  "additionalProperties": false,
  "required": ["schemas"]
}
```

## CI/CD setup
This repo also features two CI/CD pipelines: one based on [Travis CI](https://travis-ci.com/) (configured by [.travis.yml](.travis.yml)) 
and one based on [GitHub Actions](https://github.com/features/actions) (configured by [ci.yml](.github/workflows/ci-yml))
This is for demonstrational purpose only: typically the developers maintaining a repository will choose only one CI/CD solution.

In this case, the GitHub Action workflow serves as a demonstration for the [Schema CI Github Action](https://github.com/snowplow-product/msc-schema-ci-action),
while the Travis CI pipeline serves as a universal example for non-GitHub CI/CD solutions.

### GitHub Actions
The GitHub Actions workflow setup consists in adding the [Schema CI Github Action](https://github.com/snowplow-product/msc-schema-ci-action) as a step of a job.
See [ci.yml](.github/workflows/ci-yml) for an example and the Action's repo for reference and configuration.

### Universal (e.g. Travis CI, CircleCI, Gitlab, Azure Pipelines, Jenkins...)
The universal/generic approach (for non-GitHub CI/CD solutions) is based on an executable Java artifact (JAR) which can be run directly as a CLI application.
See [.travis.yml](.travis.yml) for an example
