package org.openelisglobal.observationhistorytype.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.observationhistorytype.dao.ObservationHistoryTypeDAO;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;

@Service
public class ObservationHistoryTypeServiceImpl extends BaseObjectServiceImpl<ObservationHistoryType, String> implements ObservationHistoryTypeService {
	@Autowired
	protected ObservationHistoryTypeDAO baseObjectDAO;

	public ObservationHistoryTypeServiceImpl() {
		super(ObservationHistoryType.class);
	}

	@Override
	protected ObservationHistoryTypeDAO getBaseObjectDAO() {
		return baseObjectDAO;
	}

	@Override
	@Transactional(readOnly = true)
	public ObservationHistoryType getByName(String name) {
		return getBaseObjectDAO().getByName(name);
	}
}