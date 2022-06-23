(ns heraldicon.frontend.entity.core
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub ::public?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :access)]))

  (fn [access [_ _path]]
    (= access :public)))

(rf/reg-sub ::saved?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :id)]))

  (fn [entity-id [_ _path]]
    entity-id))

(rf/reg-sub ::owned-by?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj path :username)]))

  (fn [username [_ _path session]]
    (= username (:username session))))
