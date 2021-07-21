(ns heraldry.frontend.ui.element.attribution
  (:require [heraldry.options :as options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [heraldry.attribution :as attribution]
            [re-frame.core :as rf]))

(rf/reg-sub :attribution-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[attribution options] [_ _path]]
    (let [sanitized-attribution (options/sanitize attribution options)
          main-name (case (:nature sanitized-attribution)
                      :own-work "Own work"
                      :derivative "Derivative")
          license (:license attribution)
          changes [main-name
                   (if (= license :none)
                     "no license"
                     (attribution/license-display-name license (:license-version attribution)))]]
      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn attribution-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:attribution-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label link-name {:width "30em"}
         [:<>
          (for [option [:nature
                        :license
                        :license-version]]
            ^{:key option} [interface/form-element (conj path option)])

          [:div {:style {:height "1.5em"}}]

          (for [option [:source-license
                        :source-license-version
                        :source-name
                        :source-link
                        :source-creator-name
                        :source-creator-link]]
            ^{:key option} [interface/form-element (conj path option)])]]]])))

(defmethod interface/form-element :attribution [path]
  [attribution-submenu path])

