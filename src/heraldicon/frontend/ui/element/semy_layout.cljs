(ns heraldicon.frontend.ui.element.semy-layout
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]
   [re-frame.core :as rf]))

;; TODO: probably can be improved with better subscriptions
(rf/reg-sub :semy-layout-submenu-link-name
  (fn [[_ context] _]
    [(rf/subscribe [:get context])
     (rf/subscribe [:heraldicon.state/options (:path context)])])

  (fn [[layout options] [_ _path]]
    (let [sanitized-layout (options/sanitize layout options)
          main-name (string/str-tr (:num-fields-x sanitized-layout) "x"
                                   (:num-fields-y sanitized-layout)
                                   " "
                                   :string.miscellaneous/fields)
          changes [main-name
                   (when (some #(options/changed? % sanitized-layout options)
                               [:offset-x :offset-y])
                     :string.submenu-summary/shifted)
                   (when (some #(options/changed? % sanitized-layout options)
                               [:stretch-x :stretch-y])
                     :string.submenu-summary/stretched)
                   (when (options/changed? :rotation sanitized-layout options)
                     :string.submenu-summary/rotated)]]
      (string/upper-case-first (string/combine ", " changes)))))

(defn layout-submenu [context]
  (when-let [options (interface/get-relevant-options context)]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:semy-layout-submenu-link-name context])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "22em"}
                                                       :class "submenu-semy-layout"}
         (ui.interface/form-elements
          context
          [:num-fields-x
           :num-fields-y
           :offset-x
           :offset-y
           :stretch-x
           :stretch-y
           :rotation])]]])))

(defmethod ui.interface/form-element :semy-layout [context]
  [layout-submenu context])
