(ns heraldry.frontend.ui.element.position
  (:require
   [heraldry.coat-of-arms.position :as position]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.options :as options]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :position-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[position options] [_ _path]]
    (let [sanitized-position (options/sanitize position options)
          changes [(-> sanitized-position
                       :point
                       position/anchor-point-map)
                   (when (some #(options/changed? % sanitized-position options)
                               [:offset-x :offset-y :angle])
                     "adjusted")
                   (when (options/changed? :alignment sanitized-position options)
                     "aligned")]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn position-submenu [{:keys [path] :as context}]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:position-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label link-name {:style {:width "22em"}
                                                  :class "submenu-position"}
         (ui-interface/form-elements
          context
          [:point
           :alignment
           :angle
           :offset-x
           :offset-y
           :type])]]])))

(defmethod ui-interface/form-element :position [context]
  [position-submenu context])
