(ns heraldry.frontend.form
  (:require [re-frame.core :as rf]))

(defn field [db-path function]
  (let [value @(rf/subscribe [:get db-path])
        error @(rf/subscribe [:get-form-error db-path])]
    [:div {:class (when error "error")}
     (when error
       [:div.error-message error])
     (function :value value
               :on-change #(let [new-value (-> % .-target .-value)]
                             (rf/dispatch [:set db-path new-value])))]))
