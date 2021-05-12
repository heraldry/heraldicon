(ns heraldry.frontend.form.semy
  (:require [heraldry.coat-of-arms.semy.options :as semy-options]
            [heraldry.frontend.form.charge :as charge]
            [heraldry.frontend.form.element :as element]
            heraldry.frontend.form.state
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]))

(defn form [path & {:keys [parent-field
                           form-for-field
                           form-for-layout]}]
  (let [data @(rf/subscribe [:get path])
        options semy-options/default-options]
    [element/component path :field (-> data :charge :type
                                       util/translate-cap-first) "Semy"
     [:div.settings
      (when (:layout options)
        [form-for-layout path :options (:layout options)])]
     [charge/form (conj path :charge)
      :parent-field parent-field
      :form-for-field form-for-field
      :part-of-semy? true]]))
