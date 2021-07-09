(ns heraldry.frontend.ui.element.fimbriation
  (:require [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(rf/reg-sub :fimbriation-title
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [fimbriation [_ _path]]
    ;; TODO: needs to get real options or directly sanitized data
    (let [effective-data (options/sanitize fimbriation (fimbriation/options fimbriation))]
      (case (:mode effective-data)
        :none "None"
        :single (str (-> effective-data :tincture-1 util/translate-tincture util/upper-case-first))
        :double (str (-> effective-data :tincture-1 util/translate-tincture util/upper-case-first)
                     " and " (-> effective-data :tincture-2 util/translate-tincture util/upper-case-first))))))

(defn fimbriation-submenu [path]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          title @(rf/subscribe [:fimbriation-title path])]
      [:div.ui-setting
       (when label
         [:label label])
       [:div.option
        [submenu/submenu path label title {:width "30em"}
         (for [option [:mode
                       :alignment
                       :corner
                       :thickness-1
                       :tincture-1
                       :thickness-2
                       :tincture-2]]
           ^{:key option} [interface/form-element (conj path option)])]]])))

(defmethod interface/form-element :fimbriation [path]
  [fimbriation-submenu path])
