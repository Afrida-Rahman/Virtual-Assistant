package org.mmh.virtual_assistant.core

import android.content.Context
import org.mmh.virtual_assistant.exercise.home.*
import org.mmh.virtual_assistant.exercise.home.ankle.AROMAnkleDorsiflexionInSitting
import org.mmh.virtual_assistant.exercise.home.ankle.AnklePumpInSupine
import org.mmh.virtual_assistant.exercise.home.ankle.HeelSlides
import org.mmh.virtual_assistant.exercise.home.back.*
import org.mmh.virtual_assistant.exercise.home.elbow.*
import org.mmh.virtual_assistant.exercise.home.fitness.*
import org.mmh.virtual_assistant.exercise.home.functional.*
import org.mmh.virtual_assistant.exercise.home.golf.*
import org.mmh.virtual_assistant.exercise.home.hip.*
import org.mmh.virtual_assistant.exercise.home.knee.*
import org.mmh.virtual_assistant.exercise.home.neck.*
import org.mmh.virtual_assistant.exercise.home.shoulder.*
import org.mmh.virtual_assistant.exercise.home.yoga.*

object Exercises {

    fun get(context: Context): List<HomeExercise> {
        return listOf(
            ArmRaiseInStanding(context),
            BodyWeightSquats(context),
//            HalfSquat(context),
            KneeExtensionInSitting(context),
            PelvicBridgeInSupine(context),
            IsometricCervicalExtensionInSitting(context),
            LateralBendingStretchInStanding(context),
            TrunkFlexionInStanding(context),
            BirdDogInQuadruped(context),
            LumberFlexionInSitting(context),
            SingleLegRaiseInQuadruped(context),
            SingleLegRaiseInProne(context),
            ProneOnElbows(context),
            SingleArmRaiseInProne(context),
            SingleArmRaiseInQuadruped(context),
            Quadruped(context),
            PronePressUpLumbar(context),
            PlankOnElbowsInProne(context),
            SingleLegFallOutInSupine(context),
            TrunkRotationInSitting(context),
            TrunkRotationInStanding(context),
            PlankOnKneesInProne(context),
            IsometricShoulderAdductionInStanding(context),
            IsometricCervicalExtensionInStanding(context),
            HamstringCurlsInProne(context),
            IsometricCervicalFlexionInStanding(context),
            PosteriorPelvicTiltInSupine(context),
            HamstringCurlsInStanding(context),
            BirdDogInProne(context),
            PlankOnHandsInProne(context),
            HamstringStretchInLongSitting(context),
            SeatedRowsTheraband(context),
            IsometricShoulderAdductionInSitting(context),
            IsometricCervicalFlexionInSitting(context),
            IsometricCervicalRotationInStanding(context),
            ShoulderFlexionWithWeightsInStanding(context),
            ShoulderExtensionWithWeightsInStanding(context),
            AROMAnkleDorsiflexionInSitting(context),
            WallSquatsWithBallSqueeze(context),
            WallSquatsWithStabilityBall(context),
            ScapularStabilisationStabilityBallSingleHand(context),
            ScapularStabilizationStabilityBallBothHands(context),
            TrunkExtensionOnHandInProne(context),
            Crunches(context),
            SingleKneeToChestInSupine(context),
            AROMHipAbductionInStanding(context),
            IsometricCervicalRotationInSitting(context),
            SingleArmAndLegRaiseInProne(context),
            TrunkFlexionInSitting(context),
            DoubleKneeToChestInSupine(context),
            TrunkLateralBendingInSitting(context),
            SingleLegRaiseWithWeightsInProne(context),
            CervicalFlexionStretchInSitting(context),
            CervicalExtensionStretchInSitting(context),
            CervicalLateralBendingStretchInSitting(context),
            TrunkFlexionStretchingInStanding(context),
            TrunkExtensionStretchingInStanding(context),
            TrunkLateralBendingInStanding(context),
            TrunkRotationInSupine(context),
            DoubleLegFallOutInSupine(context),
            IsometricCervicalLateralBendingInStanding(context),
            IsometricCervicalLateralBendingInSitting(context),
            CervicalLateralBendingWithResistanceBandInSitting(context),
            TrunkExtensionWithHandsOnHipsInStanding(context),
            AROMCervicalExtensionInSitting(context),
            AROMCervicalFlexionInSitting(context),
            AROMCervicalFlexionInSupine(context),
            KneeExtensionWithResistanceBandInSitting(context),
            HamstringCurlsWithWeightsInProne(context),
            HamstringCurlsWithResistanceBandInProne(context),
            KneeFlexionWithResistanceBandInSitting(context),
            KneeFlexionWithResistanceBandInLongSitting(context),
            KneeFlexionWithResistanceBandInStanding(context),
            KneeExtensionWithResistanceBandInStanding(context),
            LungesWithWeights(context),
            PelvicBridgeWithStraightLegRaiseInSupine(context),
            SingleArmRaiseWithWeightsInQuadruped(context),
            HipAbductionWithWeightsInStanding(context),
            HipExtensionWithWeightsInStanding(context),
            HipFlexionWithWeightsInSitting(context),
            HipFlexionWithWeightsInStanding(context),
            WallSquats(context),
            ShoulderOverheadPressWithWeightsInSitting(context),
            ShoulderFlexionWithResistanceBandInStanding(context),
            SeatedRowsWithResistanceBandInLongSitting(context),
            PelvicBridgeWithBallSqueezeInSupine(context),
            SitToStand(context),
            SingleArmRaiseWithWeightsInProne(context),
            AROMCervicalLateralBendingInSitting(context),
            WallAngelsInStanding(context),
            ShoulderAbductionWithWeightsInStanding(context),
            ResistedElbowFlexionWithResistanceBandInSitting(context),
            ResistedElbowFlexionWithWeightsInSitting(context),
            AROMHipFlexionInSitting(context),
            SquatsWithWeights(context),
            ButterflyStretchInSitting(context),
            ModifiedFencerStretch(context),
            ActiveKneeFlexionInLongSitting(context),
            HamstringCurlsWithWeightsInStanding(context),
            ShortArcQuadsInLongSitting(context),
            JumpingForwardAndBackward(context),
            ShoulderFlexionWithDowelAndWeightsInSitting(context),
            ElbowIRWithResistanceBandInStanding(context),
            ElbowERWithResistanceBandInStanding(context),
            ElbowExtensionWithResistanceBandInSitting(context),
            SitToStandAdvance(context),
            StraightLegRaiseInSupine(context),
            HipFlexionWithWeightsInSupine(context),
            HamstringStretchWithChairInSitting(context),
            HamstringStretchWithChairInStanding(context),
            HamstringStretchInSitting(context),
            DeadBugInSupine(context),
            QuadSetsWithTowelInSupine(context),
            IsometricShoulderExtensionInStanding(context),
            IsometricShoulderFlexionInStanding(context),
            AROMShoulderAbductionInStanding(context),
            IsometricShoulderAbductionInStanding(context),
            AAROMShoulderFlexionWithStickInStanding(context),
            MedianNerveGlideInSitting(context),
            SingleKneeToChestHandsFrontKneeInSupine(context),
            RadialNerveGlideInSitting(context),
            ThoracicRotationWithStickInStanding(context),
            PushUpsInProne(context),
            PushUpsFromKnees(context),
            IsometricElbowExtensionInSitting(context),
            IsometricElbowFlexionInSitting(context),
            AROMHipExtensionInStanding(context),
            AROMHipFlexionInStanding(context),
            AROMHipFlexionInSupine(context),
            PassiveHamstringStretchInSitting(context),
            QuadrupedToHalfKneeling(context),
            KneelingToSquatKneeling(context),
            KneelingToHalfKneelingSupported(context),
            KneelingToHalfKneelingUnsupported(context),
            HalfKneelingToStandingSupported(context),
            HalfKneelingToStandingUnsupported(context),
            StandingToHalfKneelingSupported(context),
            StandingToHalfKneelingUnsupported(context),
            OneLeggedHingeInStanding(context),
            AdvancedBirdDog(context),
            BeginnerWallSquats(context),
            IntermediateWallSquats(context),
            KneelingSideKick(context),
            Crawling(context),
            AnklePumpInSupine(context),
            HeelSlides(context),
            CrawlingStepThroughInQuadruped(context),
            BilateralHeelSlides(context),
            WallPushUps(context),
            KneelingToHalfKneelingWithNormalSupport(context),
            HipAbductionInSideLying(context),
            HipAbductionWithWeightsInSideLying(context),
            IsometricHipAbductionInSitting(context),
            SquatsWithChair(context),
            ScapularRetractionElbowStraightWeightsStanding(context),
            ScapularRetractionElbowFlexed90DegreeWithWeightsStanding(context),
            ScapularStabilizationKneelingStabilityBallSingleHand(context),
            RightShoulderInternalRotationWithResistanceBand(context),
            PiriformisStretchInSupine(context),
            MountainClimbers(context),
            SquatsShoulderOverheadWithWeights(context),
            VerticalRowsWithResistanceBandInStanding(context),
            DeskPushUps(context),
            HipFlexorStretchInHalfKneeling(context),
            BirdDogWithWeightsInQuadruped(context),
            ModifiedSupermanInProne(context),
            ShoulderOverheadPressWithWeightsForearmNeutralInSitting(context),
            FunctionalSeries1(context),
            Rite1(context),
            Rite2(context),
            Rite3(context),
            Rite4(context),
            Rite5(context),
            OneLeggedHingeRightInStanding(context),
            OneLeggedSquats(context),
            SwimmerInProne(context),
            IsometricCervicalCombinationStrengtheningInSitting(context),
            IsometricCervicalCombinationStrengtheningInStanding(context),
            CervicalFlexionWithResistanceBandInSitting(context),
            CervicalExtensionWithResistanceBandInSitting(context),
            ClassicBicepsCurlInStanding(context),
            HammerCurlsInStanding(context),
            OverheadPulleysInSitting(context),
            WallClimbInStanding(context),
            PreacherCurlInStanding(context),
            ShortHeadBicepsCurlInStanding(context),
            ClassicBicepsCurlInSitting(context),
            HammerCurlsInSitting(context),
            ShortHeadBicepsCurlInSitting(context),
            LongHeadBicepsCurlInSitting(context),
            SidePlankWithElbowFlexed(context),
            MiniSquatsWithResistanceBand(context),
            ShoulderSupportedIr90DegreesElbowFlexionWithResistanceBandInStanding(context),
            ShoulderSupportedEr90DegreesElbowFlexionWithResistanceBandInStanding(context),
            SupermanInProne(context),
            SidePlankWithElbowExtended(context),
            ToeUps(context),
            TrunkRotationInHalfKneeling(context),
            SplitSquats(context),
            BenchPressWithWeightsInSupineWithKneesFlexed(context),
            BendOverRowsWithWeightsInTripod(context),
            BendOverRowsRightWithWeightsInTripod(context),
            RightArmWallClimbingInStanding(context),
            RightPreacherCurlsInStanding(context),
            RightTrunkRotationInHalfKneeling(context),
            LateralRaisesWithWeightsInStanding(context)
        )
    }

    fun get(context: Context, exerciseId: Int): HomeExercise? {
        return get(context).find { it.id == exerciseId }
    }
}