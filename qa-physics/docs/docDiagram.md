# QA Timeline Production Flowchart

## Legend
```mermaid
flowchart TB
    classDef data fill:#ff8,color:black
    classDef auto fill:#8f8,color:black
    classDef manual fill:#fbb,color:black
    classDef timeline fill:#8af,color:black
    classDef json fill:#d5d,color:black

    data{{Data files}}:::data
    timeline{{Timeline<br/>HIPO files}}:::timeline
    subgraph "Wrapper Script"
      auto[Automated step,<br/>by specified Wrapper Script]:::auto
    end

    manual[Manual step,<br/>not automated]:::manual
    json([JSON file]):::json
    data --> auto
    data --> manual
    auto --> timeline
    manual --> timeline
    auto --> json
```

## Flowchart

Note: output directories `$output_dir` and `$qa_dir` are typically set by wrapper scripts, and may vary depending on how they are run.

```mermaid
flowchart TB

    classDef data fill:#ff8,color:black
    classDef auto fill:#8f8,color:black
    classDef manual fill:#fbb,color:black
    classDef timeline fill:#8af,color:black
    classDef json fill:#d5d,color:black

    subgraph "Automated by qtl histogram"
      dst{{DSTs}}:::data
      monitorRead[monitorRead.groovy]:::auto
      monitorReadOut{{$output_dir/data_table_$run.dat<br>$output_dir/monitor_$run.hipo}}:::data
      dst --> monitorRead
      monitorRead --> monitorReadOut
    end

    subgraph "Automated by ../bin/qtl physics"
      datasetOrganize[datasetOrganize.sh]:::auto
      outmonFiles{{$qa_dir/outmon/monitor_$run.hipo}}:::data
      outdatFiles{{$qa_dir/outdat/data_table.dat}}:::data
      monitorReadOut --> datasetOrganize
      datasetOrganize --> outmonFiles
      datasetOrganize --> outdatFiles

      buildCT[buildChargeTree.groovy]:::auto
      chargeTree([$qa_dir/outdat/chargeTree.json]):::json
      outmonFiles --> buildCT
      outdatFiles --> buildCT

      qaPlot[qaPlot.groovy]:::auto
      outdatFiles --> draw_epochs[draw_epochs.sh<br />draw_epochs.C]:::manual
      draw_epochs --> createEpochs[create or edit<br>qadb/epochs/epochs.$dataset.txt]:::manual
      monitorElec{{$qa_dir/outmon/monitorElec.hipo}}:::data
      outdatFiles --> qaPlot
      qaPlot --> monitorElec

      qaCut[qaCut.groovy]:::auto
      mergeFTandFD[mergeFTandFD.groovy]:::auto
      qaTreeFD([$qa_dir/outdat/qaTreeFD.json]):::json
      qaTreeFT([$qa_dir/outdat/qaTreeFT.json]):::json
      qaTreeFTandFD([$qa_dir/outdat/qaTreeFTandFD.json]):::json
      timelineFiles{{$qa_dir/outmon/$timeline.hipo}}:::timeline
      monitorElec --> qaCut
      createEpochs --> qaCut
      qaCut --> timelineFiles
      qaCut --> qaTreeFD --> mergeFTandFD
      qaCut --> qaTreeFT --> mergeFTandFD
      mergeFTandFD --> qaTreeFTandFD

      monitorPlot[monitorPlot.groovy]:::auto
      qaTree([$qa_dir/outdat/qaTree.json]):::json
      outmonFiles --> monitorPlot
      qaTreeFTandFD --> monitorPlot
      monitorPlot --> qaTree
      monitorPlot --> timelineFiles

      stage0[stageTimelines.sh]:::auto
      buildCT --> chargeTree
      timelineFiles --> stage0
    end

    qaTree --> manualQA[perform the manual QA]:::manual

    subgraph "Finalize (deprecated)"
      exeQAtimelines[exeQAtimelines.sh]:::manual
      qaTreeUpdated([$qa_dir/outdat/qaTree.json]):::json
      qaTL{{$qa_dir/outmon.qa/$timeline.hipo}}:::timeline
      stage1[stageTimelines.sh]:::manual
      manualQA --> exeQAtimelines
      exeQAtimelines --> qaTL
      exeQAtimelines -->|updates|qaTreeUpdated
      qaTL --> stage1
    end
```
