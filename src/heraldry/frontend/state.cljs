(ns heraldry.frontend.state
  (:require [clojure.string :as s]
            [re-frame.core :as rf]))

;; subs

(rf/reg-sub
 :get
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-sub
 :get-form-error
 (fn [db [_ path]]
   (get-in db (concat [:form-errors] path [:message]))))

;; events

(rf/reg-event-db
 :set
 (fn [db [_ path value]]
   (assoc-in db path value)))

(rf/reg-event-db
 :remove
 (fn [db [_ path]]
   (cond-> db
     (-> path count (= 1)) (dissoc (first path))
     (-> path count (> 1)) (update-in (drop-last path) dissoc (last path)))))

(rf/reg-event-db
 :set-form-error
 (fn [db [_ db-path error]]
   (assoc-in db (concat [:form-errors] db-path [:message]) error)))

(rf/reg-event-fx
 :clear-form-errors
 (fn [_ [_ db-path]]
   {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]]}))

(rf/reg-event-fx
 :clear-form
 (fn [_ [_ db-path]]
   {:fx [[:dispatch [:remove (into [:form-errors] db-path)]]
         [:dispatch [:remove db-path]]]}))

;; other


(defn path []
  @(rf/subscribe [:get [:path]]))

(defn path-extra []
  (let [path-extra @(rf/subscribe [:get [:path-extra]])]
    (when (-> path-extra count (> 0))
      path-extra)))

(defn set-path [path & [hash]]
  (let [path (if hash
               (str path hash)
               path)
        chunks (s/split path #"#" 2)
        path (first chunks)
        path-extra (second chunks)]
    (rf/dispatch-sync [:set [:path] path])
    (rf/dispatch-sync [:set [:path-extra] path-extra])))

(defn goto [path]
  (set-path path)
  (js/window.history.pushState "" nil path))
