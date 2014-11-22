/*
 *
 * Copyright (C) 2014
 *
 */

package de.htw.sdf.photoplatform.repository.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.Transactional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Repository;

import de.htw.sdf.photoplatform.persistence.models.Role;
import de.htw.sdf.photoplatform.persistence.models.User;
import de.htw.sdf.photoplatform.repository.UserDAO;
import de.htw.sdf.photoplatform.repository.common.GenericDAOImpl;

/**
 * repository methods for users.
 *
 * @author <a href="mailto:s0541962@htw-berlin.de">Vincent Schwarzer</a>
 */
@Repository
@Transactional
public class UserDAOImpl extends GenericDAOImpl<User> implements UserDAO,
        UserDetailsService {
    /**
     * User DAO constructor.
     */
    public UserDAOImpl() {
        super();
        setClazz(User.class);
    }

    public void create(User entity) {
        super.create(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User findByUserName(String userName) {
        String queryString = "SELECT DISTINCT(user) FROM User user "
                + "LEFT JOIN FETCH user.userRoles userRoles "
                + "WHERE user.username like ?1";

        Query query = createQuery(queryString);
        query.setParameter(1, userName);

        try
        {
            return (User) query.getSingleResult();
        }
        catch (NoResultException ex)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> find(Integer start, Integer count) {
        StringBuilder queryBuilder = initSelectQuery();
        queryBuilder.append("LEFT JOIN FETCH user.userBank ");
        queryBuilder.append("WHERE user.enabled = true ");
        queryBuilder.append("ORDER BY user.username ");
        Query query = createQuery(queryBuilder.toString());
        query.setFirstResult(start.intValue());
        query.setMaxResults(count.intValue());
        return (List<User>) query.getResultList();
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public User findByEmail(String email)
    {
        Query query = createQuery("SELECT u FROM User u WHERE u.email = :email");
        query.setParameter("email", email);

        try
        {
            return (User) query.getSingleResult();
        }
        catch (NoResultException ex)
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDetails loadUserByUsername(final String username) {
        String queryString = "SELECT user FROM User user "
                + "LEFT JOIN FETCH user.userRoles userRoles "
                + "WHERE user.username like ?1";

        Query query = createQuery(queryString);
        query.setParameter(1, username);
        return (User) query.getSingleResult();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> findByRole(final Role role) {
        return findByRoleId(role.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> findByRoleId(final Long roleId) {
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT user FROM User user ");
        queryBuilder.append("LEFT JOIN FETCH user.userRoles userRoles ");
        if (Role.PHOTOGRAPHER_ID.equals(roleId)) {
            queryBuilder.append("LEFT JOIN FETCH user.userBank ");
        }
        queryBuilder.append("WHERE userRoles.role.id = ?1");

        Query query = createQuery(queryBuilder.toString());
        query.setParameter(1, roleId);
        return (List<User>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> findAllNotAdminUsers() {
        StringBuilder queryBuilder = initSelectQuery();
        queryBuilder.append("WHERE userRoles.role.id != ?1");

        Query query = createQuery(queryBuilder.toString());
        query.setParameter(1, Role.ADMIN_ID);
        return (List<User>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> findByEnabled(boolean enabled) {
        StringBuilder queryBuilder = initSelectQuery();
        queryBuilder.append("WHERE user.enabled = ?1");

        Query query = createQuery(queryBuilder.toString());
        query.setParameter(1, enabled);
        return (List<User>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    public List<User> findByRoleAndEnabledFilter(Long roleId, boolean enabled){
        StringBuilder queryBuilder = initSelectQuery();
        if (Role.PHOTOGRAPHER_ID.equals(roleId)) {
            queryBuilder.append("LEFT JOIN FETCH user.userBank ");
        }
        queryBuilder.append("WHERE userRoles.role.id = :roleId ");
        queryBuilder.append("AND user.enabled = :enabled");

        Query query = createQuery(queryBuilder.toString());
        query.setParameter("roleId", roleId);
        query.setParameter("enabled", enabled);
        return (List<User>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> findByAccountLocked(boolean locked) {
        StringBuilder queryBuilder = initSelectQuery();
        queryBuilder.append("WHERE user.accountNonLocked = ?1");

        Query query = createQuery(queryBuilder.toString());
        query.setParameter(1, locked);
        return (List<User>) query.getResultList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public User findById(Long id) {
        StringBuilder queryBuilder = initSelectQuery();
        queryBuilder.append("LEFT JOIN FETCH user.userBank ");
        queryBuilder.append("WHERE user.id = ?1");
        Query query = createQuery(queryBuilder.toString());
        query.setParameter(1, id);
        return (User) query.getSingleResult();
    }

    private StringBuilder initSelectQuery() {
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT DISTINCT(user) FROM User user ");
        queryBuilder.append("LEFT JOIN FETCH user.userRoles userRoles ");
        queryBuilder.append("LEFT JOIN FETCH user.userProfile ");

        return queryBuilder;
    }

    @Override public EntityManager getEntityManager() {
        return super.getEntityManager();
    }
}
