#ifndef _HDMI_COMMON_H_
#define _HDMI_COMMON_H_

#include <HdmiConstDef.h>

typedef struct hdmi_module_callback {

    /**
     * callback to handle hdmi event
     */
    void (*handle_event)(
        const struct hdmi_module_callback* callback,
        int32_t event);

} hdmi_module_callback_t;

typedef struct hdcp_module_callback {

    // basic callback of a HDCP module
    void (*handle_event)(const struct hdcp_module_callback* callback);

} hdcp_module_callback_t;


#endif
