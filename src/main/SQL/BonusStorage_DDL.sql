USE BonusStorage

IF EXISTS (SELECT 1 FROM SYS.TABLES where name ='BonusesInDocuments')
BEGIN
DROP TABLE BonusesInDocuments;
END;
IF EXISTS (SELECT 1 FROM SYS.TABLES where name ='Documents')
BEGIN
DROP TABLE Documents;
END;
IF EXISTS (SELECT 1 FROM SYS.TABLES where name ='DocumentTypes')
BEGIN
DROP TABLE DocumentTypes;
END;
IF EXISTS (SELECT 1 FROM SYS.TABLES where name ='OperationTypes')
BEGIN
DROP TABLE OperationTypes;
END;
IF EXISTS (SELECT 1 FROM SYS.TABLES where name ='SourceBases')
BEGIN
DROP TABLE SourceBases;
END;




----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE SourceBases(
                            Id INT IDENTITY NOT NULL PRIMARY KEY,
                            Name VARCHAR(255) NOT NULL,
                            ConnectString VARCHAR(1000),
                            CurrentVersionBonusesProcessed BIGINT --для процедуры заполнения бонуса
);
--INSERT INTO SourceBases (Name,CurrentVersionBonusesProcessed) VALUES ('retail_2017_stockmann',0x000000000C6DFF66);
INSERT INTO SourceBases (Name,ConnectString,CurrentVersionBonusesProcessed) VALUES ('bonus_temp_marina','DB01001.bonus_temp_marina.[dbo].',0);

SELECT * FROM SourceBases;

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE OperationTypes(
                               Id INT IDENTITY NOT NULL PRIMARY KEY,
                               Name VARCHAR(500) NOT NULL,
);
INSERT INTO OperationTypes (Name) VALUES ('Списание бонусов за покупку в универмаге');
INSERT INTO OperationTypes (Name) VALUES ('Начисление бонусов за покупку в универмаге');
INSERT INTO OperationTypes (Name) VALUES ('Списание бонусов за оформление заказа');
INSERT INTO OperationTypes (Name) VALUES ('Начисление бонусов за выкуп заказа');
INSERT INTO OperationTypes (Name) VALUES ('Возврат списанных бонусов за возврат товара, купленного в универмаге');
INSERT INTO OperationTypes (Name) VALUES ('Возврат списанных бонусов за возврат товара по заказу');
INSERT INTO OperationTypes (Name) VALUES ('Списание начисленных бонусов за возврат товара, купленного в универмаге');
INSERT INTO OperationTypes (Name) VALUES ('Списание начисленных бонусов за возврат товара по заказу');
INSERT INTO OperationTypes (Name) VALUES ('Подарочные бонусные баллы');
INSERT INTO OperationTypes (Name) VALUES ('Ручная корректировка');
INSERT INTO OperationTypes (Name) VALUES ('Списание бонусных баллов при отсутствии продаж');

SELECT * FROM OperationTypes;

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
CREATE TABLE DocumentTypes (
                               Id INT IDENTITY NOT NULL PRIMARY KEY,
                               SourceBase INT NOT NULL,
                               DocumentName VARCHAR(255) NOT NULL,
                               SourceTable VARCHAR(255) NOT NULL,
                               CONSTRAINT FK_DocumentTypes_SourceBase FOREIGN KEY (SourceBase) REFERENCES SourceBases (Id)
);

INSERT INTO DocumentTypes (SourceBase,DocumentName, SourceTable) VALUES
                                                                     (1,'ЧекККМ', '_Document223'),
                                                                     (1,'РеализацияТоваров', '_Document203'),
                                                                     (1,'ОтчетОРозничныхПродажах', '_Document185'),
                                                                     (1,'НачислениеИСписаниеБонусныхБаллов', '_Document177'),
                                                                     (1,'КорректировкаРегистров', '_Document174'),
                                                                     (1,'ЗаказПокупателя', '_Document164'),
                                                                     (1,'ВозвратТоваровОтПокупателя', '_Document158');


SELECT * FROM DocumentTypes;

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE Documents(
                          Id INT IDENTITY NOT NULL PRIMARY KEY,
                          SourceBase INT NOT NULL,
                          DocumentType INT NOT NULL,
                          Ext_IDRRef BINARY(16) NOT NULL,
                          OneCId CHAR(36),
                          Ext_Date_Time DATETIME2(0) NOT NULL,
                          Ext_Number VARCHAR(50) NOT NULL,
                          CurrentVersion BIGINT NOT NULL,
                          IsChanged BIT NOT NULL,
                          Created DATETIME2(0),
                          Updated DATETIME2(0),
                          BonusesUploaded DATETIME2(0),
                          CONSTRAINT FK_Documents_SourceBase FOREIGN KEY (SourceBase) REFERENCES SourceBases (Id),
                          CONSTRAINT FK_Documents_DocumentType FOREIGN KEY (DocumentType) REFERENCES DocumentTypes (Id),
                          INDEX NIndex_Documents_SourceBase_DocumentType_CurrentVersion NONCLUSTERED (SourceBase, DocumentType, CurrentVersion DESC),
                          INDEX NIndex_Documents_OneCId NONCLUSTERED (OneCId)
);

SELECT * FROM Documents;

----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------

CREATE TABLE BonusesInDocuments(
                                   Id INT IDENTITY NOT NULL PRIMARY KEY,
                                   SourceBase INT NOT NULL,
                                   DocumentType INT NOT NULL,
                                   Document INT NOT NULL,
                                   StoreId BINARY(16),
                                   StoreName VARCHAR (100),
                                   CardNumber VARCHAR (100) NOT NULL,
                                   TypeOfIncrement VARCHAR (10) NOT NULL,
                                   Value FLOAT NOT NULL,
                                   TypeOfOperation INT NOT NULL,
                                   TextOperation VARCHAR (500),
                                   OrderID VARCHAR(50),
                                   BonusStartDate DATETIME2(0) NOT NULL,
                                   BonusEndDate DATETIME2(0),
                                   Created DATETIME2(0),
                                   CONSTRAINT FK_BonusesInDocuments_SourceBase FOREIGN KEY (SourceBase) REFERENCES SourceBases (Id),
                                   CONSTRAINT FK_BonusesInDocuments_DocumentType FOREIGN KEY (DocumentType) REFERENCES DocumentTypes (Id),
                                   CONSTRAINT FK_BonusesInDocuments_Document FOREIGN KEY (Document) REFERENCES Documents (Id),
                                   INDEX NIndex_BonusesInDocuments_Document NONCLUSTERED (Document),
                                   INDEX NIndex_BonusesInDocuments_CardNumber NONCLUSTERED (CardNumber)
);



CREATE NONCLUSTERED INDEX IX_Documents_CurrentVersion
ON dbo.Documents (CurrentVersion ASC)
INCLUDE (Id, DocumentType, OneCId, Ext_Number, Ext_Date_Time);



USE BonusStorage;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.tables
    WHERE name = 'SMS_informed'
)
BEGIN
CREATE TABLE dbo.SMS_informed (
                                  Id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
                                  Document INT NOT NULL,
                                  CurrentVersion BIGINT NOT NULL,
                                  EventsCount BIGINT NOT NULL,
                                  InformedAt DATETIME2(0) NOT NULL
                                      CONSTRAINT DF_SMS_informed_InformedAt DEFAULT SYSDATETIME(),

                                  CONSTRAINT FK_SMS_informed_Document
                                      FOREIGN KEY (Document) REFERENCES dbo.Documents(Id)
);

CREATE UNIQUE NONCLUSTERED INDEX UX_SMS_informed_Document
        ON dbo.SMS_informed(Document);

    CREATE NONCLUSTERED INDEX IX_SMS_informed_CurrentVersion
        ON dbo.SMS_informed(CurrentVersion);
END;
GO

USE BonusStorage;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'IX_Documents_CurrentVersion_SMS'
      AND object_id = OBJECT_ID('dbo.Documents')
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_Documents_CurrentVersion_SMS
    ON dbo.Documents (CurrentVersion ASC)
    INCLUDE (
        Id,
        DocumentType,
        OneCId,
        Ext_Number,
        Ext_Date_Time
    );
END;
GO