@echo off
@echo ���ڴ�����ռ估�û�.....
sqlplus sys/orcl@orcl  as sysdba @1createSpaceAndUser.sql>log.txt

@echo ������񼰳�ʼ��.....
sqlplus agile/agile@orcl @createAndInitDB.sql>log.txt

@pause 