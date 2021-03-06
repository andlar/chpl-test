@Regression
Feature: OCD-1937 - Update Listings to have "cleaned" data fields
  Tests in this feature will be used to verify updated text for these fields based on fuzzy matching technique using provided mapping of values.

  Scenario Outline: Verify text update for QMS Standard after bulk update
    Given I am on listing details page of listing with ID "<DB_ID>"
    When I open details for criteria "<CRITERIA>"
    Then QMS Standard should display "<Text>" for criteria "<CRITERIA>"
    Examples:
      | DB_ID | CRITERIA       | Text            |
      |  8566 | 170.315 (g)(4) | Home Grown      |
      |  8556 | 170.315 (g)(4) | 21 CFR Part 820 |
      |  8490 | 170.314 (g)(4) | Home Grown      |
      |  9378 | 170.314 (g)(4) | 21 CFR Part 820 |

  Scenario Outline: Verify text update for Accessibility Standard  after bulk update
    Given I am on listing details page of listing with ID "<DB_ID>"
    When I open details for criteria "<CRITERIA>"
    Then Accessibility Standard for criteria "<CRITERIA>" should display "<Text>"
    Examples:
      | DB_ID | CRITERIA       | Text              |
      |  8556 | 170.315 (g)(5) | WCAG Level        |
      |  7981 | 170.315 (g)(5) | WCAG 2.0 Level AA |

  Scenario Outline: Verify text update for UCD Process text after bulk update
    Given I am on listing details page of listing with ID "<DB_ID>"
    When I look at SED details
    Then UCD process should display "<Text>"
    Examples:
      | DB_ID | Text         |
      |  9058 | ISO 9241-210 |
      |  8871 | NISTIR 7741  |
