package com.likelion.yonsei.baton.domain.modellab.entity;

/** Eval dataset split (spec section 12). SMOKE for fast iteration, CORE for daily dev, HOLDOUT for final comparison. */
public enum DatasetSplit {
	SMOKE,
	CORE,
	HOLDOUT
}
