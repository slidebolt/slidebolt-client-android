Feature: Connect a generic SlideBolt client

  Scenario: Discover and validate an installation
    Given a SlideBolt installation named "Test Home"
    When the client loads its well-known contract
    Then the stable system id is "system-test"
    And pairing is advertised

  Scenario: Pair after administrator approval
    Given a SlideBolt installation named "Test Home"
    And the administrator will approve pairing
    When the client requests and exchanges a pairing code
    Then the paired access is "control"
    And the paired security groups include "*"

  Scenario: Pending pairing remains pending
    Given a SlideBolt installation named "Test Home"
    When the client requests a pairing code without approval
    Then the exchange status is pending

  Scenario: Inventory remains provider neutral
    Given an approved client for a SlideBolt installation
    When the client loads devices and entities
    Then it receives a "light" entity with command "light_turn_on"
    And it does not need plugin-specific decoding
