package ${project_package}

import griffon.annotations.core.Nonnull
import griffon.annotations.inject.MVCMember
import griffon.core.artifact.GriffonView
import org.kordamp.jipsy.ServiceProviderFor
import java.beans.PropertyChangeListener
import org.codehaus.griffon.runtime.lanterna.artifact.AbstractLanternaGriffonView

@ServiceProviderFor(GriffonView)
class ${project_class_name}View extends AbstractLanternaGriffonView {
    @MVCMember @Nonnull
    FactoryBuilderSupport builder

    @MVCMember @Nonnull
    ${project_class_name}Model model

    void mvcGroupInit(Map<String, Object> args) {
        model.addPropertyChangeListener('clickCount', { evt ->
            builder.clickLabel.text = model.clickCount
        } as PropertyChangeListener)
        builder.clickLabel.text = model.clickCount
    }

    void initUI() {
        builder.with {
            application(id: '${name}') {
                verticalLayout()
                label(id: 'clickLabel')
                button(clickAction)
            }
        }
    }
}