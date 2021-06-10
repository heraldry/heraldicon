(ns heraldry.frontend.form.charge-group
  (:require [heraldry.frontend.form.element :as element]
            [re-frame.core :as rf]))

(defn form [path & {:keys [parent-field form-for-field part-of-semy?]}]
  (let [charge-group @(rf/subscribe [:get path])
        title "Test"]
    [element/component path :charge-group title nil
     [:div.settings]]))
