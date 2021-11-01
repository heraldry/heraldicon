(ns heraldry.frontend.ui.element.fimbriation
  (:require
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as interface]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(rf/reg-sub :fimbriation-submenu-link-name
  (fn [[_ path] _]
    [(rf/subscribe [:get path])
     (rf/subscribe [:get-relevant-options path])])

  (fn [[fimbriation options] [_ _path]]
    (let [sanitized-fimbriation (options/sanitize fimbriation options)
          main-name (case (:mode sanitized-fimbriation)
                      :none strings/none
                      :single (util/str-tr (-> sanitized-fimbriation
                                               :tincture-1
                                               tincture/translate-tincture
                                               util/upper-case-first))
                      :double (util/str-tr (-> sanitized-fimbriation
                                               :tincture-1
                                               tincture/translate-tincture
                                               util/upper-case-first)
                                           " "
                                           strings/and
                                           " "
                                           (-> sanitized-fimbriation
                                               :tincture-2
                                               tincture/translate-tincture
                                               util/upper-case-first)))
          changes [main-name
                   (when (some #(options/changed? % sanitized-fimbriation options)
                               [:alignment :thickness-1 :thickness-2])
                     strings/adjusted)]]

      (-> (util/combine ", " changes)
          util/upper-case-first))))

(defn fimbriation-submenu [{:keys [path] :as context}]
  (when-let [options @(rf/subscribe [:get-relevant-options path])]
    (let [{:keys [ui]} options
          label (:label ui)
          link-name @(rf/subscribe [:fimbriation-submenu-link-name path])]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu path label link-name {:style {:width "22em"}
                                               :class "submenu-fimbriation"}
         (for [option [:mode
                       :alignment
                       :corner
                       :thickness-1
                       :tincture-1
                       :thickness-2
                       :tincture-2]]
           ^{:key option} [interface/form-element (c/++ context option)])]]])))

(defmethod interface/form-element :fimbriation [context]
  [fimbriation-submenu context])
