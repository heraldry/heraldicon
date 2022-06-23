(ns heraldicon.frontend.user.session
  (:require
   [heraldicon.config :as config]
   [heraldicon.frontend.repository.core :as repository]
   [heraldicon.util.url :as url]
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

(defn- set-storage-item [key value]
  (if value
    (hp/set-item hp/local-storage key value)
    (hp/remove-item hp/local-storage key)))

(defn- get-storage-item [key]
  (hp/get-item hp/local-storage key))

(defn read-from-storage []
  (let [session-id (get-storage-item local-storage-session-id-name)
        user-id (get-storage-item local-storage-user-id-name)
        username (get-storage-item local-storage-username-name)]
    (rf/dispatch-sync [::store
                       (when (and session-id
                                  username
                                  user-id)
                         {:username username
                          :session-id session-id
                          :user-id user-id})])))

(rf/reg-fx ::set-cookie
  (fn [[session-id]]
    (let [relevant-url (or (config/get :heraldicon-site-url)
                           (config/get :heraldicon-url))]
      (if (some-> session-id count pos?)
        (set! js/document.cookie (str "session-id=" session-id
                                      ";domain=" (url/domain relevant-url)
                                      ";path=/"))
        (set! js/document.cookie (str "session-id="
                                      ";domain=" (url/domain relevant-url)
                                      ";path=/"
                                      ";Max-Age=-99999999"))))))

(rf/reg-fx ::write-to-local-storage
  (fn [[session-id username user-id]]
    (set-storage-item local-storage-session-id-name session-id)
    (set-storage-item local-storage-username-name username)
    (set-storage-item local-storage-user-id-name user-id)))

(rf/reg-event-fx ::store
  (fn [{:keys [db]} [_ {:keys [session-id
                               username
                               user-id]}]]
    {:db (assoc-in db db-path {:username username
                               :session-id session-id
                               :user-id user-id
                               :logged-in? (boolean (and username
                                                         session-id
                                                         user-id))})
     ::set-cookie [session-id]
     ::write-to-local-storage [session-id username user-id]}))

(rf/reg-event-fx ::clear
  (fn [{:keys [db]} _]
    {:db (assoc-in db db-path nil)
     ::set-cookie [nil]
     ::write-to-local-storage [nil nil nil]}))

(rf/reg-sub ::logged-in?
  (fn [_ _]
    (rf/subscribe [:get (conj db-path :logged-in?)]))

  (fn [logged-in? _]
    logged-in?))

(defn data-from-db [db]
  (get-in db db-path))

(rf/reg-sub ::data
  (fn [_ _]
    (rf/subscribe [:get db-path]))

  (fn [logged-in? _]
    logged-in?))

(rf/reg-event-fx ::logout
  (fn [_ _]
    {:dispatch-n [[::repository/clear-lists]
                  [::clear]]
     ::set-cookie [nil]
     ::write-to-local-storage [nil nil nil]}))
