package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors

/** Single rounded bone used inside screen skeletons. */
@Composable
fun SkeletonBone(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    cornerRadius: Dp = 8.dp,
) {
    val tc = LocalTruckColors.current
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tc.SurfaceSecondary),
    )
}

/**
 * Load detail loading placeholder that mirrors route card + stat rows
 * so navigation doesn't flash an empty spinner.
 */
@Composable
fun LoadDetailSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shimmerPulse()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.72f), height = 20.dp)
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.45f), height = 14.dp)
            }
        }
        StatRowSkeleton()
        StatRowSkeleton(columns = 2)
        StatRowSkeleton(columns = 2)
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.4f), height = 18.dp)
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.9f), height = 12.dp)
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.8f), height = 12.dp)
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.85f), height = 12.dp)
            }
        }
    }
}

/**
 * Analytics loading placeholder: period chip row + summary cards + chart block.
 */
@Composable
fun AnalyticsSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shimmerPulse()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                SkeletonBone(
                    modifier = Modifier.weight(1f),
                    height = 36.dp,
                    cornerRadius = 18.dp,
                )
            }
        }
        StatRowSkeleton(columns = 2)
        StatRowSkeleton(columns = 2)
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.5f), height = 18.dp)
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.7f), height = 12.dp)
                SkeletonBone(
                    modifier = Modifier.fillMaxWidth(),
                    height = 160.dp,
                    cornerRadius = 12.dp,
                )
            }
        }
        StatsCardSkeleton(modifier = Modifier.fillMaxWidth())
        StatsCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatRowSkeleton(columns: Int = 3) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(columns) {
            BentoGlassCard(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBone(modifier = Modifier.fillMaxWidth(0.7f), height = 12.dp)
                    SkeletonBone(modifier = Modifier.fillMaxWidth(0.9f), height = 20.dp)
                }
            }
        }
    }
}


/**
 * Edit-load form placeholder: stacked field bones matching the edit screen layout.
 */
@Composable
fun EditLoadSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shimmerPulse()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBone(modifier = Modifier.fillMaxWidth(0.55f), height = 14.dp)
        repeat(5) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SkeletonBone(modifier = Modifier.fillMaxWidth(0.35f), height = 12.dp)
                    SkeletonBone(modifier = Modifier.fillMaxWidth(), height = 44.dp, cornerRadius = 12.dp)
                }
            }
        }
    }
}

/**
 * Weekly goal placeholder: hero ring + summary cards.
 */
@Composable
fun WeeklyGoalSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shimmerPulse()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SkeletonBone(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(bottom = 8.dp),
                    height = 18.dp,
                )
                SkeletonBone(
                    modifier = Modifier.fillMaxWidth(),
                    height = 160.dp,
                    cornerRadius = 80.dp,
                )
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.4f), height = 22.dp)
                SkeletonBone(modifier = Modifier.fillMaxWidth(0.6f), height = 14.dp)
            }
        }
        StatRowSkeleton(columns = 2)
        StatsCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Map placeholder: chip row + large map plate.
 */
@Composable
fun MapSkeleton(
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shimmerPulse()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                SkeletonBone(
                    modifier = Modifier.weight(1f),
                    height = 36.dp,
                    cornerRadius = 18.dp,
                )
            }
        }
        SkeletonBone(
            modifier = Modifier.fillMaxWidth(),
            height = 280.dp,
            cornerRadius = 16.dp,
        )
        StatsCardSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Generic list-of-cards placeholder (attach picker / journals).
 */
@Composable
fun LoadListSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 4,
    contentDescription: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shimmerPulse()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(rows) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SkeletonBone(modifier = Modifier.fillMaxWidth(0.65f), height = 16.dp)
                    SkeletonBone(modifier = Modifier.fillMaxWidth(0.4f), height = 12.dp)
                    SkeletonBone(modifier = Modifier.fillMaxWidth(0.85f), height = 12.dp)
                }
            }
        }
    }
}
