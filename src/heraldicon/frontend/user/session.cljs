(ns heraldicon.frontend.user.session
  (:require
   [heraldicon.entity.user :as user]
   [heraldicon.frontend.repository.core :as repository]
   [hodgepodge.core :as hp]
   [re-frame.core :as rf]))

(def ^:private db-path
  [:session])

(def ^:private local-storage-session-id-name
  "cl-session-id")

(def ^:private local-storage-user-id-name
  "cl-user-id")

(def ^:private local-storage-username-name
  "cl-username")

(def ^:private local-storage-dark-mode-name
  "cl-dark-mode")

(defn- set-storage-item [key value]
  (if value
    (hp/set-item hp/local-storage key value)
    (hp/remove-item hp/local-storage key)))

(defn- get-storage-item [key]
  (hp/get-item hp/local-storage key))

(defn read-from-storage []
  (let [session-id (get-storage-item local-storage-session-id-name)
        user-id (get-storage-item local-storage-user-id-name)
        username (get-storage-item local-storage-username-name)
        dark-mode? (= (get-storage-item local-storage-dark-mode-name) "true")]
    (rf/dispatch-sync [::store {:username username
                                :session-id session-id
                                :user-id user-id
                                :dark-mode? dark-mode?}])))

(rf/reg-fx ::set-cookie
  (fn [[session-id]]
    (if (some-> session-id count pos?)
      (set! js/document.cookie (str "session-id=" session-id
                                    ";path=/"))
      (set! js/document.cookie (str "session-id="
                                    ";path=/"
                                    ";Max-Age=-99999999")))))

(rf/reg-fx ::write-to-local-storage
  (fn [[{:keys [session-id username user-id dark-mode?]}]]
    (set-storage-item local-storage-session-id-name session-id)
    (set-storage-item local-storage-username-name username)
    (set-storage-item local-storage-user-id-name user-id)
    (set-storage-item local-storage-dark-mode-name (when dark-mode?
                                                     "true"))))

(rf/reg-event-fx ::store
  (fn [{:keys [db]} [_ session-data]]
    {:db (assoc-in db db-path session-data)
     ::set-cookie [(:session-id session-data)]
     ::write-to-local-storage [session-data]}))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} _]
    {:db (assoc-in db db-path nil)
     ::set-cookie [nil]
     ::write-to-local-storage [nil]}))

(rf/reg-sub ::logged-in?
  (fn [_ _]
    [(rf/subscribe [:get (conj db-path :session-id)])
     (rf/subscribe [:get (conj db-path :username)])
     (rf/subscribe [:get (conj db-path :user-id)])])

  (fn [[session-id username user-id] _]
    (and session-id username user-id)))

(rf/reg-sub ::dark-mode?
  (fn [_ _]
    (rf/subscribe [:get (conj db-path :dark-mode?)]))

  (fn [dark-mode? _]
    dark-mode?))

(defn data-from-db [db]
  (get-in db db-path))

(rf/reg-sub ::data
  (fn [_ _]
    (rf/subscribe [:get db-path]))

  (fn [data _]
    data))

(rf/reg-sub ::admin?
  (fn [_ _]
    (rf/subscribe [:get db-path]))

  (fn [data _]
    (user/admin? data)))

(rf/reg-event-fx ::logout
  (fn [_ _]
    {:dispatch-n [[::clear]
                  [::repository/session-change]]
     ::set-cookie [nil]
     ::write-to-local-storage [nil]}))

(rf/reg-event-fx ::toggle-dark-mode
  (fn [{:keys [db]} _]
    (let [new-db (update-in db (conj db-path :dark-mode?) not)
          session-data (get-in new-db db-path)]
      {:db new-db
       ::write-to-local-storage [session-data]})))
