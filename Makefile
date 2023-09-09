build: monitoring detectors

.PHONY: clean
clean: monitoring-clean detectors-clean

#######################################################

define header =
@echo ""
@echo "============================================="
@echo ">>> $(1)"
@echo "============================================="
endef

monitoring detectors: FORCE
	$(call header,BUILD $@)
	@cd $@ && mvn package

%-clean:
	$(call header,CLEAN $*)
	@cd $* && mvn clean

FORCE:
