# Run Group K, Fall 2023 and Spring 2024, Pass 1

We will use DST files from 3 datasets; we will keep them separate, to be consistent with what we did for Fall 2018 RG-K data:

| Time        | Beam Energy | Dataset Name      | Data Path                                                                |
| ---         | ---         | ---               | ---                                                                      |
| Fall 2023   | 6.4 GeV     | `rgk_fa23_6.4GeV` | `/cache/clas12/rg-k/production/recon/fall2023/pass1/6395MeV/dst/recon`   |
| Spring 2024 | 6.4 GeV     | `rgk_sp24_6.4GeV` | `/cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon` |
| Spring 2024 | 8.5 GeV     | `rgk_sp24_8.5GeV` | `/cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon` |

## Run monitoring

> [!IMPORTANT]
> Check any run-dependent settings in `qa-physics/monitorRead.groovy`, such as beam energy.

check cache:
```bash
bin/qtl histogram -d rgk_fa23_6.4GeV --dstdir --check-cache --focus-physics /cache/clas12/rg-k/production/recon/fall2023/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_6.4GeV --dstdir --check-cache --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_8.5GeV --dstdir --check-cache --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon
```

run:
```bash
bin/qtl histogram -d rgk_fa23_6.4GeV --dstdir --check-charge --fast-ls --submit --focus-physics /cache/clas12/rg-k/production/recon/fall2023/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_6.4GeV --dstdir --check-charge --fast-ls --submit --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/6395MeV/dst/recon
bin/qtl histogram -d rgk_sp24_8.5GeV --dstdir --check-charge --fast-ls --submit --focus-physics /cache/clas12/rg-k/production/recon/spring2024/pass1/8477MeV/dst/recon
```

## Checks

### Inspect the Charge Analysis Plots

Inspect the charge analysis plots (`.png` files), to see if we need to reheat.

You can download the `.png` files to make this easier; _e.g._, `tar` them with
```bash
tar cavf outfiles/rgk_fa23_6.4GeV_charge_analysis.tar.gz $(find outfiles/rgk_fa23_6.4GeV -type f -name '*.png')
tar cavf outfiles/rgk_sp24_6.4GeV_charge_analysis.tar.gz $(find outfiles/rgk_sp24_6.4GeV -type f -name '*.png')
tar cavf outfiles/rgk_sp24_8.5GeV_charge_analysis.tar.gz $(find outfiles/rgk_sp24_8.5GeV -type f -name '*.png')
```
then `scp` them to your local machine.

### Run List

Double check that we have _all_ the runs, and that all DSTs are still there, in case any disappeared from `/cache` _while_ jobs were running; simply
re-run the above `qtl histogram --check-cache` commands.

## Make timelines

Make the timelines, separately for each of the 2 datasets:
```bash
bin/qtl physics -d rgk_fa23_6.4GeV -p rgk/pass1/qa/rgk_fa23_6.4GeV
bin/qtl physics -d rgk_sp24_6.4GeV -p rgk/pass1/qa/rgk_sp24_6.4GeV
bin/qtl physics -d rgk_sp24_8.5GeV -p rgk/pass1/qa/rgk_sp24_8.5GeV
```

> [!WARNING]
> You may not have permission to publish to this directory; if not, publish to another place and ask the chef to copy the files

# Run Lists

For reference, here are the lists of runs with DST-file directories:

## Fall 2023, 6.4 GeV
```
19204
19205
19206
19208
19209
19210
19211
19212
19213
19214
19215
19216
19217
19218
19219
19220
19222
19223
19224
19225
19226
19228
19229
19231
19232
19233
19234
19235
19236
19237
19238
19239
19243
19244
19245
19246
19247
19248
19249
19250
19251
19252
19253
19254
19256
19257
19258
19259
19260
```

## Spring 2024, 6.4 GeV
```
19308
19316
19317
19318
19319
19321
19322
19323
19324
19325
19326
19327
19328
19329
19330
19331
19333
19334
19335
19336
19337
19338
19339
19340
19341
19342
19343
19344
19345
19346
19347
19348
19349
19351
19352
19353
19354
19355
19357
19359
19360
19361
19362
19363
19364
19365
19366
19367
19382
19384
19385
19386
19387
19388
19389
19390
19391
19392
19393
19394
19395
19399
19400
19401
19403
19405
19406
19407
19409
19410
19411
19413
19414
19415
19416
19417
19418
19419
19420
19421
19422
19423
19424
19425
19426
19427
19428
19429
19430
19431
19432
19433
19434
19435
19436
19437
19438
19439
19440
19441
19442
19443
19444
19446
19447
19448
19449
19450
19451
19452
19453
19454
19455
19456
19457
19458
19459
19461
19463
19464
19465
19466
19467
19468
19469
19470
19471
19472
19473
19475
19476
19477
19478
19479
19480
19481
19482
19483
19485
19486
19487
19488
19489
19490
19491
19492
19493
19494
19495
19496
19497
19498
19499
19500
19501
19502
19503
19504
19505
19506
19507
19509
19511
19512
19513
19515
19517
19518
19519
19520
19521
19522
19523
19524
19526
19527
19528
19535
19538
19539
19540
19541
19543
19544
19545
19546
19547
19548
19549
19550
19551
19552
19553
19554
19555
19556
19557
19558
19559
19561
19562
19563
19564
19565
19566
19567
19568
19569
19571
19572
19573
19574
19575
19576
19577
19578
19579
19580
19581
19582
19583
19584
19585
19586
19587
19588
19589
19590
19591
19592
19593
19594
19595
19596
19597
19598
19599
19600
19601
19602
19603
19604
19605
19606
19607
19608
19609
19610
19611
19612
19614
19615
19616
19617
19618
19620
19621
19623
19624
19625
19626
19627
19628
19629
19630
19631
19632
19633
19634
19635
19636
19637
19638
19639
19640
19642
19643
19644
19645
19646
19647
19648
19649
19650
19651
19653
19654
19655
19656
19657
19658
19659
```

## Spring 2024, 8.5 GeV
```
19660
19662
19663
19664
19665
19666
19668
19669
19670
19671
19676
19677
19678
19679
19680
19683
19684
19685
19686
19688
19689
19690
19692
19693
19694
19695
19696
19697
19698
19699
19700
19701
19702
19703
19704
19705
19706
19707
19708
19709
19710
19711
19713
19715
19720
19721
19722
19723
19726
19727
19728
19729
19730
19731
19743
19744
19745
19746
19747
19748
19749
19750
19751
19752
19753
19754
19755
19756
19757
19758
19759
19760
19762
19763
19764
19765
19766
19767
19768
19769
19770
19771
19772
19773
19774
19775
19776
19777
19778
19779
19780
19781
19782
19783
19785
19786
19787
19788
19789
19793
19794
19795
19796
19797
19798
19799
19800
19801
19802
19803
19805
19806
19807
19810
19811
19812
19813
19814
19815
19817
19818
19819
19820
19821
19822
19823
19824
19825
19826
19827
19828
19829
19830
19831
19832
19833
19834
19835
19836
19837
19838
19839
19841
19842
19845
19846
19847
19848
19849
19850
19851
19852
19853
19854
19855
19856
19857
19860
19861
19863
19864
19865
19866
19867
19868
19869
19870
19871
19872
19873
19874
19875
19876
19877
19878
19879
19880
19881
19882
19883
19884
19885
19886
19887
19888
19889
19890
19891
19892
19893
```
