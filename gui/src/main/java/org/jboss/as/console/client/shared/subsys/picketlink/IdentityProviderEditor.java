/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.console.client.shared.subsys.picketlink;

import com.google.common.collect.Iterables;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.layout.OneToOneLayout;
import org.jboss.as.console.client.v3.dmr.ResourceDescription;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.ComboBoxItem;
import org.jboss.ballroom.client.widgets.forms.FormCallback;
import org.jboss.ballroom.client.widgets.forms.FormItem;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.forms.FormValidator;
import org.jboss.dmr.client.ModelNode;
import org.useware.kernel.gui.behaviour.StatementContext;

import java.util.List;
import java.util.Map;

/**
 * @author Harald Pehl
 */
class IdentityProviderEditor implements IsWidget {

    final FederationPresenter presenter;
    final SecurityContext securityContext;
    final StatementContext statementContext;
    final ResourceDescription resourceDescription;

    ComboBoxItem securityDomains;
    ModelNodeFormBuilder.FormAssets formAssets;

    IdentityProviderEditor(final FederationPresenter presenter,
            final SecurityContext securityContext,
            final StatementContext statementContext,
            final ResourceDescription resourceDescription) {
        this.securityContext = securityContext;
        this.statementContext = statementContext;
        this.resourceDescription = resourceDescription;
        this.presenter = presenter;
    }

    @Override
    public Widget asWidget() {
        OneToOneLayout layout = new OneToOneLayout()
                .setPlain(true)
                .setHeadline("Identity Provider")
                .setDescription(Console.CONSTANTS.identityProviderDescription())
                .setDetail(Console.CONSTANTS.common_label_attributes(), formPanel());
        return layout.build();
    }

    Widget formPanel() {
        securityDomains = new ComboBoxItem("security-domain", "Security Domain");
        securityDomains.setRequired(false);

        formAssets = new ModelNodeFormBuilder()
                .setConfigOnly()
                .addFactory("security-domain", attributeDescription -> securityDomains)
                .setResourceDescription(resourceDescription)
                .setSecurityContext(securityContext).build();
        formAssets.getForm().addFormValidator(new IdentityProviderFormValidator());
        formAssets.getForm().setToolsCallback(new FormCallback() {
            @Override
            public void onSave(Map changeSet) {
                presenter.modifyIdentityProvider(formAssets.getForm().getChangedValues());
            }

            @Override
            public void onCancel(Object entity) {
                formAssets.getForm().cancel();
            }
        });

        VerticalPanel formPanel = new VerticalPanel();
        formPanel.setStyleName("fill-layout-width");
        formPanel.add(formAssets.getHelp().asWidget());
        formPanel.add(formAssets.getForm().asWidget());
        return formPanel;
    }

    void update(ModelNode identityProvider, final List<String> securityDomains) {
        this.securityDomains.setValueMap(securityDomains);
        this.formAssets.getForm().edit(identityProvider);
    }


    /**
     * Validates that security domain is set for non-external identity provider
     */
    public static class IdentityProviderFormValidator implements FormValidator {

        @Override
        public void validate(List<FormItem> formItems, FormValidation outcome) {
            FormItem securityDomain = findItem("security-domain", formItems);
            FormItem external = findItem("external", formItems);

            if (securityDomain == null || external == null) {
                return;
            }

            if (!Boolean.TRUE.equals(external.getValue())
                    && (securityDomain.isUndefined() || "".equals(securityDomain.getValue()))) {
                String origErrMessage = securityDomain.getErrMessage();
                securityDomain.setErrMessage(Console.MESSAGES.domainMandatoryForNonExternalProvider());
                securityDomain.setErroneous(true);
                securityDomain.setErrMessage(origErrMessage);
                outcome.addError("security-domain");
            }
        }

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        private FormItem findItem(String name, List<FormItem> formItems) {
            return Iterables.find(formItems, formItem -> name.equals(formItem.getName()));
        }
    }
}
