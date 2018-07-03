package com.salesmanager.core.business.services.user;

import java.util.List;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.user.User;



public class UserServiceLDAPImpl implements UserService {

	@Override
	public void save(User entity) throws ServiceException {
		throw new ServiceException("Not implemented");

	}

	@Override
	public void update(User entity) throws ServiceException {
		throw new ServiceException("Not implemented");

	}

	@Override
	public void create(User entity) throws ServiceException {
		throw new ServiceException("Not implemented");

	}

	@Override
	public void delete(User entity) throws ServiceException {
		throw new ServiceException("Not implemented");

	}



	@Override
	public User getById(Long id) {
		return null;
	}

	@Override
	public List<User> list() {
        return null;
    }



	@Override
	public Long count() {
        return null;
    }





	@Override
	public User getByUserName(String userName) throws ServiceException {
        throw new ServiceException("Not implemented");
    }

	@Override
	public List<User> listUser() throws ServiceException {
		throw new ServiceException("Not implemented");
	}

	@Override
	public void saveOrUpdate(User user) throws ServiceException {
		throw new ServiceException("Not implemented");

	}

	@Override
	public List<User> listByStore(MerchantStore store)
			throws ServiceException {
		throw new ServiceException("Not implemented");
	}

    @Override
    public void flush() {
        // TODO Auto-generated method stub

    }



}
