(ns heraldry.frontend.ui.element.pin-arms-checkbox
  (:require [heraldry.frontend.ui.element.checkbox :as checkbox]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defn get-current-version [arms-info]
  ;; TODO: dummy to test it
  8)

(rf/reg-event-db :set-pin-arms
  (fn [db [_ path value]]
    (let [arms-reference-path (-> path
                                  drop-last
                                  vec
                                  (conj :reference))]
      (-> db
          (assoc-in path value)
          (cond->
           (not value) (assoc-in (conj arms-reference-path :version) 0)
           value (assoc-in (conj arms-reference-path :version)
                           (get-current-version
                            (get-in db arms-reference-path))))))))

(defmethod interface/form-element :pin-arms-checkbox [path]
  [checkbox/checkbox path
   :on-change (fn [value]
                (rf/dispatch [:set-pin-arms path value]))])
