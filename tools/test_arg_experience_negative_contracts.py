#!/usr/bin/env python3
"""Mutation self-test for every fail-closed ARG experience/input guardrail."""
from __future__ import annotations

from copy import deepcopy

from check_arg_experience_authority import guardrail_violations


BASELINE = {
    "conclusion_printed_by_one_source": False,
    "minimum_case_surface_count": 3,
    "correctness_requires_receipt": False,
    "minimum_player_action_count": 2,
    "minimum_world_response_count": 1,
    "answer_box_dominates_adjacent_cases": False,
    "major_story_optional": False,
    "named_player_eligibility": False,
    "unsafe_personalized_copy": False,
    "lectern_text_input": False,
    "decorative_fake_form": False,
    "required_chat_parser": False,
    "input_absent_from_pinned_platform": False,
    "client_input_overflow": False,
    "decoded_plaintext_needlessly_opaque": False,
    "source_click_prerequisite": False,
}


MUTATIONS = {
    "conclusion_printed_by_one_source": ("conclusion_printed_by_one_source", True),
    "single_surface_bounded_case": ("minimum_case_surface_count", 1),
    "click_or_receipt_gated_correctness": ("correctness_requires_receipt", True),
    "interaction_free_story": ("minimum_player_action_count", 0),
    "no_player_caused_response": ("minimum_world_response_count", 0),
    "repeated_answer_box_grammar": ("answer_box_dominates_adjacent_cases", True),
    "optional_major_side_story": ("major_story_optional", True),
    "absent_named_player_gate": ("named_player_eligibility", True),
    "unsafe_or_unbounded_personalized_copy": ("unsafe_personalized_copy", True),
    "lectern_text_input": ("lectern_text_input", True),
    "decorative_fake_form": ("decorative_fake_form", True),
    "required_unrestricted_chat_parser": ("required_chat_parser", True),
    "input_absent_from_pinned_platform": ("input_absent_from_pinned_platform", True),
    "book_sign_or_dialog_overflow": ("client_input_overflow", True),
    "needlessly_opaque_decoded_plaintext": ("decoded_plaintext_needlessly_opaque", True),
    "source_click_prerequisite": ("source_click_prerequisite", True),
}


def main() -> None:
    if guardrail_violations(BASELINE):
        raise AssertionError(f"baseline should pass: {guardrail_violations(BASELINE)}")
    for expected, (field, value) in MUTATIONS.items():
        candidate = deepcopy(BASELINE)
        candidate[field] = value
        violations = guardrail_violations(candidate)
        if violations != [expected]:
            raise AssertionError(f"{expected}: expected exact rejection, got {violations}")
    print(f"ARG EXPERIENCE NEGATIVE CONTRACTS: PASS - {len(MUTATIONS)} fail-closed mutations rejected")


if __name__ == "__main__":
    main()
