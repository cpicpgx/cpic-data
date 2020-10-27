package org.cpicpgx.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FunctionReferenceImporterTest {

  @Test
  public void testParseAlleleDefinitionName() {
    assertNull(FunctionReferenceImporter.parseAlleleDefinitionName(""));
    assertNull(FunctionReferenceImporter.parseAlleleDefinitionName("    "));

    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2"));
    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2xN"));
    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2x3"));
    assertEquals("*2", FunctionReferenceImporter.parseAlleleDefinitionName("*2≥2"));
    assertEquals("c.61G>T", FunctionReferenceImporter.parseAlleleDefinitionName("c.61G>T"));
    assertEquals("c.61G>T", FunctionReferenceImporter.parseAlleleDefinitionName("c.61G>Tx2"));
    assertEquals("c.61G>T", FunctionReferenceImporter.parseAlleleDefinitionName("c.61G>T   x2"));
    assertEquals("H1", FunctionReferenceImporter.parseAlleleDefinitionName("H1"));
  }

  private static final String FINDINGS = "23752738: S-warfarin (in vitro), tolbutamide (in vitro); 10413320: phenytoin (in vitro); 8004131: S-warfarin (in vivo); 12496751: S-warfarin (in vivo); 21110013: phenprocoumon (in vivo); 11668218: phenytoin (in vivo); 9698079: fluribiprofen (in vitro) Lower Vmax value for oxidation of S-flurbiprofen than *1 (minor differences between *2 and *1); 22547083: fluribiprofen (in vitro) *1 and *2 have similar Km values, but *2 has a reduced Vmax compared to *1. Intrinsic clearance is substantially lower for *2 than *1; 25144335: fluribiprofen (in vitro) 61.36% relative clearance compared to *1; 25775139: fluribiprofen (in vivo) measured metabolic ratio for flurbiprofen and the *1/*2 genotype was 85% of that of *1/*1, measured metabolic ratio for flurbiprofen and the *2/*2 genotype was 70% of that of *1/*1, measured metabolic ratio for flurbiprofen and the *2/*3 genotype was 36% of that of *1/*1; 27298492: fluribiprofen (in vivo) *1/*2 and *2/*3 subjects had a significantly increased metabolic ratio compared to *1/*1; 12520632: fluribiprofen (in vivo) Clearance was reduced in *1/*2 subjects and single *2/*2 subject. Carrying a *2 or *3 allele had a statistically significant effect on fluribiprofen clearance; 12698304: fluribiprofen (in vivo) *1/*2 subjects had flurbiprofen AUC 1.4-fold higher than *1/*1 (not statistically significant) and significantly longer flurbiprofen elimination half-life than *1/*1. Clearance of flurbiprofen (oral and formation of 4-hydroxy metabolite) was lower in *1/*2 subjects than *1/*1 (not statistically significant); 9698079: warfarin (in vitro) Lower Vmax value for oxidation of S-warfarin than *1 (minor differences between *2 and *1); 9686881: warfarin (in vivo) One patient was homozygous for this allele. Their S-warfarin/R-warfarin ratio was increased compared to *1 (0.87:1 for *2 v 0.56:1 for *1) One patient was homozygous for this allele. Their S-warfarin/R-warfarin ratio was increased compared to *1 (0.87:1 for *2 v 0.56:1 for *1); 15824753: warfarin (in vivo) Subjects were grouped by the number of *2 or *3 alleles carried (i.e. 0, 1 or 2 alleles). Warfarin and R-warfarin plasma levels were significantly higher in *1/*1 than in carriers of *2 or *3 alleles. No effect of *2 or *3 on S-warfarin plasma levels. S:R concentration level increases as number of *2 or *3 alleles increases; 17895500: warfarin (in vivo) S/R warfarin concentration ratio increased with the number of *2 or *3 alleles *1/*2<*1/*3<*2/*2<*2/*3; 24322786: S-warfarin (in vitro) Vmax, Km and intrinsic clearance were reduced in *2 compared to *1; 27199745: S-warfarin (in vitro) Significantly reduced intrinsic clearance of S-warfarin compared to *1; 12621390: S-warfarin (in vivo) *2 allele was not found in any Japanese patients. Caucasian *1/*2 and *2/*2 patients showed no significant difference in oral clearance compared to *1/*1. One Caucasian *2/*3 patient had a 70% reduction in oral clearance and an >90% reduction in rate of S-warfarin 7-hydroxylation compared to *1/*1; 9522436: warfarin (in vitro) *2 has similar rate of hydroxylation of warfarin compared to WT; 21148049: warfarin (in vivo) *2/*3 subjects had a significantly increased S/R warfarin ratio compared to *1/*1 and *1/*2 subjects; 12621390: S-warfarin (in vivo) *2 allele was not found in any Japanese patients. Caucasian *1/*2 and *2/*2 patients showed no significant difference in oral clearance compared to *1/*1. One Caucasian *2/*3 patient had a 70% reduction in oral clearance and an >90% reduction in rate of S-warfarin 7-hydroxylation compared to *1/*1; 9522436: warfarin (in vitro) *2 has similar rate of hydroxylation of warfarin compared to WT; 21148049: warfarin (in vivo) *2/*3 subjects had a significantly increased S/R warfarin ratio compared to *1/*1 and *1/*2 subjects; 12426520: tolbutamide (in vivo) Significant 1.5 fold increase tolbutamide AUC in *1/*2 volunteers compared to *1/*1. Significant reduction (~29% of *1/*1 value) in clearance in *1/*2. Reduced recovery of tolbutamide in urine in *1/*2 compared to *1/*1; 19298642: tolbutamide (in vitro) Cell microsomes with *2 allele had sevenfold lower Vmax for tolbutamide hydroxylation compared to *1. Small increase in Km for tolbutamide hydroxylation. Overall effect of *2 in cell microsomes was 15-fold decrease in tolbutamide clearance. No statistical difference in hydroxylation rates for tolbutamide between *1 liver microsomes and *1/*2 liver microsomes; 8873200: tolbutamide (in vitro) No significant difference in Km, significantly decreased Vmax compared to WT; 24077631: tolbutamide (in vitro) 47.24% relative clearance compared to *1; 22547083: tolbutamide (in vitro) *1 and *2 have similar Km values, but *2 has a reduced Vmax compared to *1. Intrinsic clearance is higher for *2 than *1; 25075423: tolbutamide (in vitro) 47.88% relative clearance compared to *1; 12520632: tolbutamide (in vivo) Clearance was reduced in *1/*2 subjects and single *2/*2 subject. Carrying a *2 or *3 allele had a statistically significant effect on tolbutamide clearance; 11875364: tolbutamide (in vivo) Significantly reduced oral clearance of tolbutamide in *2/*3 volunteers compared to *1/*1. No significant differences in clearance between *1/*1, *1/*2 and *2/*2 genotypes; 9522436: tolbutamide (in vitro) *2 has similar rate of hydroxylation of tolbutamide compared to WT; 27163851: phenytoin (in vitro) 94.8% relative clearance compared to *1\n" +
      "11908757: phenytoin (in vivo) Patients with the *1/*2 or *2/*2 genotypes had significantly higher plasma concentrations of phenytoin and decreased metabolic ratio of p-HPPH:phenytoin compared to *1/*1 patients; 11434505: phenytoin (in vivo) Patients carrying at least one *2 or *3 alleles required a lower dose to reach therapeutic serum concentrations of phenytoin as compared to WT patients; 16815679: phenytoin (in vivo) *1/*2 subjects had reduced (S)-p-HPPH formation compared to *1/*1, but reduction was not as great as seen in *1/*3 and *2/*3 subjects. Single *2/*3 subject had lowest (S)/(R)-p-HPPH ratio recorded in study; 21068649: phenytoin (in vivo) *1/*2 and *1/*3 subjects had significantly increased plasma levels of phenytoin compared to *1/*1. Single *2/*3 and *3/*3 subjects had increased plasma phenytoin compared to *1/*2 and *1/*3 but unable to say if this was statistically significant; 22561479: phenytoin (in vivo) *1 and *2 have similar Km values, but *2 has a reduced Vmax compared to *1. Intrinsic clearance is higher for *2 than *1; 22641027: phenytoin (in vivo) Case study of a pediatric patient with phenytoin toxicity. Plasma concentrations of phenytoin were higher than expected and the patient was found to have the genotype CYP2C9*2/*2 and CYP2C19*1/*4; 28820457: phenytoin (in vivo) No significant difference in phenytoin or p-HPPH concentrations between *1/*2, *1/*3 and *1/*1 subjects (in presence of CYP2C19 *1/*1 genotype). *2/*2 subject had significantly increased phenytoin concentrations and significantly decreased p-HPPH concentrations compared to *1/*1; 23287317: phenytoin (in vivo) Patients with the *1/*2, *1/*3 or *2/*2 genotypes had significantly higher dose corrected phenytoin levels compared to *1/*1; 10510154: phenytoin (in vivo) Patients carrying one or two *2 alleles had significantly higher trough levels of phenytoin and lower p-HPPH:phenytoin ratio following a single dose compared to *1/*1; 11026737: perazine (in vitro) 12% reduction in Vmax compared to *1. No substantial difference in Km value compared to *1\n" +
      "19298642: sulphamethoxazole (in vitro) Cell microsomes with *2 allele had fourfold lower Vmax for sulphamethoxazole N-hydroxylation compared to *1. No change in Km for sulphamethoxazole N-hydroxylation. Overall effect of *2 in cell microsomes was three fold decrease in sulphamethoxazole clearance. No statistical difference in hydroxylation rates for sulphamethoxazole between *1 liver microsomes and *1/*2 liver microsomes. Microsomes from *2/*3 patient had lowest Vmax and clearance of sulphamethoxazole; 24663076: fluoxetine (in vivo) No effect of *2 on any pharmacokinetic parameters studied\n" +
      "14726986: fluoxetine (in vitro) Increased C/D plasma concentrations of fluoxetine and norfluoxetine in *1/*2 and *1/*3 subjects compared to *1/*1. No significant difference in C/D plasma levels between *2/*2 and *2/*3 compared to *1/*2 and *1/*3; 16236141: (in vivo) Patients carrying *2 or *3 alleles had increased plasma concentrations of R-fluoxetine and of the active moiety (sum of S-fluoxetine, R-fluoxetine and S-norfluoxetine) compared to *1/*1, with those carrying the *3 allele having the highest active moiety values; 25884291: fluoxetine (in vitro) 75.51% relative clearance compared to *1; 15128047: phenprocoumon (in vivo) No significant effect of *2 allele on pharmacokinetics of R- or S-phenprocoumon. Authors note a trend toward reduced S-phenprocoumon clearance and terminal half-life with increasing number of *2 and *3 alleles. Ratio between total clearances of S- and R-phenprocoumon significantly decreased as number of *2 and *3 alleles increased. *2/*2 subjects had significantly lower AUC of 7-OH-phenprocoumon and lower formation of 6-OH-phenprocoumon; 15742978: phenprocoumon(in vitro and in vivo) 7-hydroxylation of R- and S-phenprocoumon significantly decreased as the number of *2 and *3 alleles increased in human liver microsomes. *3 caused a greater decrease than *2. No effect of *2 or *3 allele on 4-hydroxylation of either R- or S-phenprocoumon. Non-significant decrease in 6-hydroxylation of R- and S-phenprocoumon. In healthy volunteers, AUC of phenprocoumon metabolites decreased as the number of *2 or *3 alleles increased.; 15229460: tenoxicam (in vivo) No significant difference between *1/*2 and *1/*3 in AUC and CL/F of tenoxicam. Significant increase in AUC and decreased CL/F in *1/*2 subjects compared to *1/*1; 18992346: tenoxicam (in vivo) Increased AUC in carriers of *2 and *3 compared to *1. Greater increase in AUC in *3 carriers compared to *2; 17900275: clopidogrel (in vivo) AUC of the active metabolite of clopidogrel was significantly reduced in the presence of *2 and *3 alleles; 25476996: carvedoil (in vitro) 67.76% relative clearance compared to *1; 25832633: propofol (in vitro) 79.7% relative clearance compared to *1; 25924705: mestranol (in vitro) 49.98% relative clearance compared to *1; 26774055: meloxicam (in vivo) No significant difference in meloxicam Cmax between *1/*1, *1/*2 and *1/*3 subjects but there was a significant difference in AUC (AUC of *1/*1 < *1/*2 < *1/*3); 28339166: avatrombopag (in vivo) Grouped *1/*2 and *1/*3 subjects. *1/*2 and *1/*3 subjects had increased exposure to avatrombopag compared to *1/*1; 29100760: benzbromarone (in vitro) *2 enzyme was less active than *1, but more active than *3. Relative clearance of 85.86% compared to that of *1; 29273968: siponimod (in vitro) *2/*2 liver microsomes had a significantly reduced siponimod metabolic ratio compared to *1/*1 microsomes, but an increased metabolic ratio compared to *3/*3. 3-fold reduction in hydroxylated metabolite formation compared to *1/*1; 16198655: piroxicam (in vivo) No significant difference in AUC or clearance of piroxicam between *1/*2 and *1/*3 subjects. *1/*2 and *1/*3 subjects as a group had significantly increased AUC and significantly decreased clearance compared to *1/*1; 25241292: metamizole (in vivo) Carriers of the *2 or *3 alleles (grouped as 'slow' alleles) had reduced recovery of metamizole metabolites compared to WT; 12734606: diclofenac (in vivo) No significant differences urinary metabolic ratios of diclofenac:4-OH diclofenac found between *1/*2 or *2/*2 subjects compared to *1/*1; 12603175: diclofanc (in vivo) Presence of the *2 allele had no effect on the AUC of diclofenac; 12534640: diclofenac (in vivo) Presence of the *2 allele had no impact on oral clearance of diclofenac or on the plasma concentrations of diclofenac or 4'-OH- diclofenac; 11829203: diclofenac (in vitro and in vivo) No significant difference in recovery of diclofenac or 4--OH-diclofenac from urine samples or in diclofenac:4'-OH-diclofenac ratio in healthy volunteers. Large intraindividual variation and low frequency of *2 and *3 alleles in cohort meant statistical analysis couldn't be carried out; 12742136: diclofenac (in vivo) No significant difference in diclofenac:4-OH diclofenac ratio between *1/*1 and *2/*2 subjects; 25951663: diclofenac (in vitro) 89.48% relative clearance compared to *1; 22547083: diclofenac (in vitro) *1 and *2 have similar Km values, but *2 has a reduced Vmax compared to *1. Intrinsic clearance is substantially lower for *2 than *1; 24322786: diclofenac (in vitro) Similar Km but reduced Vmax and CL/F for *2 compared to *1; 11294368: diclofenac (in vivo) Trend for *1/*2 and *1/*3 subjects (grouped in study) to have reduced metabolic clearance of diclofenac (not statistically significant); 25075423: diclofenac (in vitro) 51.46% relative clearance compared to *1; 12235454: glyburide (in vivo) No significant differences in pharmacokinetics of glyburide in *1/*2 volunteers compared to *1/*1; 11956512: glyburide (in vivo) Oral clearance of glyburide in *2/*2 volunteers was ~90% of clearance in volunteers carrying *1; 12235454: gilmepiride (in vivo) No significant differences in pharmacokinetics of glimepiride in *1/*2 volunteers compared to *1/*1, expect that Tmax was significantly later in *1/2 than *1/*1; 24118918: glimepiride (in vitro) 81.5% relative clearance compared to *1; 18694831: ibuprofen (in vivo) *1/*2 had no effect of clearance of S-ibuprofen and was associated with a significant increase in R-ibuprofen clearance; 19480553: ibuprofen (in vivo) No significant effect of *2 allele alone on ibuprofen pharmacokinetics (found in high linkage with CYP2C8*3); 15606441: R-ibuprofen (in vivo) Study focused on the effect of CYP2C8 variant on ibuprofen that detected linkage between CYP2C8*3 and CYP2C9*2. Analysis of carriers and noncarriers of *2 found no difference in PK parameters of R-ibuprofen; 12152005: ibuprofen (in vivo) Effect of CYP2C9 alleles restricted to PK of S-ibuprofen and not racemic or R-ibuprofen. Clearance of S-ibuprofen decreased as number of *2 alleles on volunteers increased (decrease was very slight, not as large as seen in *3 carriers). Effect of *2 considered by authors to be 'negligible compared to effect of *3; 15289789: ibuprofen (in vivo) Presence of the *2 allele associated with reduced clearance of ibuprofen (Clearance for *2/*2 ~50% reduction compared to *1/*1). *2/*2 had 50% reduction in R-ibuprofen clearance and 41% reduction in S-ibuprofen clearance compared to *1/*1; 26122864: ibuprofen (in vivo) Carriers of the *2 allele had 18% lower clearance and increased AUC and half-life of S-ibuprofen compared to *1/*1 and *2 carriers. No significant effect of *2 on R-ibuprofen; 9296349: ibuprofen (in vitro) *2 allele reduced hydroxylation of R- and S-ibuprofen from between 7.5-17ul/min to under 5ul/min; 11823761: losartan (in vivo) Patients with the *2/*3 genotype had significantly lower Cmax of E-3174 and longer half-life of losartan and E-3174 than *1/*1 and *1/*2 patients. Patients with the *2/*2 genotype had a similar E-3174 Cmax and rate of losartan metabolism to *1/*1 patients. No significant differences in Tmax or AUC of losartan or E-3174 between genotypes. *2/*3 patient had increase in losartan AUC:E-3174 AUC ratio compared to *1/*1.  *2/*2 had a similar AUC ratio to *1/*1. Authors conclude that *2 has a small, non-significant impact on losartan metabolism; 15100169: losartan (in vivo) No significant difference in metabolic ratio of losartan:carboxy-losartan between *1/*2, *2/*2 and *1/*1; 15197523: losartan (in vivo) Non-significant increase in metabolic ratio of losartan:E3174 (losartan metabolite) in *2/*2 subjects compared to *1/*1 and *1/*2. Single *2/*3 subject was noted as having a high metabolic ratio; 23118328: losartan (in vivo) No significant effect of *2 on the pharmacokinetics of losartan; 11408373: losartan (in vitro) In losartan oxidation in yeast expression system, *2 had an intermediate intrinsic clearance rate between *1 and *3. Human liver microsomes with the *2/*2 genotype had 2- to 3-fold lower rate of losartan oxidation than *1/*1. No significant effect of *1/*2 or *2/*2 genotypes on intrinsic clearance of losartan; 23171336: losartan (in vivo) *1/*2 subjects had a increased losartan:E3174 ratio compared to *1/*1 (not statistically significant); 23844998: losartan (in vitro) 48.58% relative clearance compared to *1; 25075423 (in vitro) 52.89% relative clearance compared to *1; 12520632: losartan (in vivo) Clearance was reduced in *1/*2 subjects and single *2/*2 subject. Effect on losartan clearance was only statistically significant when single *2/*2 subject was removed from analysis; 14597963: valproic acid (in vitro) Work using recombinant enzymes showed that *2 allele causes 44-48% reduction in formation of valproic acid metabolites. Due to reduction in Vmax. No effect on Km. However, work in human liver microsomes found no difference in metabolite formation between *1/*1, *1/*2, *2/*2 or *2/*3.\n" +
      "14709614: lornoxicam (in vitro) In baculovirus-infected insect cells,  Km, Vmax and intrinsic clearance were comparable to *1. No difference in clearance between *1/2 and *1/*1 human liver microsomes.; 15606435: lornoxicam (in vivo) Significantly increased AUC and significantly reduced clearance of lornoxicam in *1/*2 and *1/*3 subjects compared to *1/*1; 17686967: naproxen (in vitro) *2 had a similar metabolite formation rate to *1 at low substrate concentrations, but a reduced metabolite formation rate at higher substrate concentrations\"\"; 12603175: celecoxib (in vivo) Presence of the *2 allele had no effect on the AUC of celecoxib; 12392591: celecoxib (in vitro) No significant differences in celecoxib pharmacokinetics found between *1 and *2 alleles in either yeast microsomes or human liver microsomes; 11337938: celecoxib (in vitro and in vivo) Decreased Vmax in insect cell microsomes compared to *1 (4.1 v 8.9 nmol/min/nmol CYP). No change in Km. 66% intrinsic clearance of *1 clearance. ~50% decrease in Vmax/Km ratio in *1/*2 human liver microsomes. Two healthy volunteers in the in vivo study were genotyped as *1/*2 and showed no significant difference in AUCs or oral clearance of celecoxib compared to WT; 12893985: celecoxib (in vivo) No significant effect of *2 on celecoxib AUC or total clearance; 23996211: celecoxib (in vivo) High linkage disequilibrium between *2 and CYP2C8*3. CYP2C9 *1/*2 subjects had a higher Cmax compared to *1/*1 but no other differences in pharmacokinetic parameters; 16153401: celecoxib (in vivo) Study of four subjects. *2 allele determined not have a noticeable effect on celecoxib PK compared to two *1/*1 patients; 16401468: celecoxib (in vivo) Presence of *2 allele was associated with increased plasma concentrations of celecoxib (note that the same paper did not find any effect of the *3 allele on plasma concentrations); 15005635: nateglinide (in vivo) *2 allele had no significant effect on nateglinide clearance; 15592327: torsemide (in vivo) No significant difference in torsemide plasma levels between *1/*2 and *1/*1 (single *2/*2 subject suggests that they may have lower hydroxylation rate of torsemide); 19005461: THC (in vivo) No significant effect on *2 allele on THC pharmacokinetic parameters; 21148049: acencoumarol (in vivo) No significant effect of *2 on acenocoumarol pharmacokinetics; 23118328: candesartan (in vivo) No significant effect of *2 on the pharmacokinetics of candesartan, telmisartan (in vivo) No significant effect of *2 on the pharmacokinetics of telmisartan, valsartan (in vivo) No significant effect of *2 on the pharmacokinetics of valsartan; 12891229: fluvastatin (in vivo) No difference in PK parameters between *1/*1, *1/*2 and *2/*2; 25142737: bosentan (in vitro) 238.57% relative clearance compared to *1";

  @Test
  public void testReformatFindings() {
    JsonObject findingsObject = (JsonObject)FunctionReferenceImporter.parseFindingsObject(FINDINGS);
    assertNotNull(findingsObject);
    assertEquals(95, findingsObject.size());
    assertEquals("bosentan (in vitro) 238.57% relative clearance compared to *1", findingsObject.get("25142737").getAsString());
  }
}
